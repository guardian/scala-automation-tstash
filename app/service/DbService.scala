package service

import java.io.File

import reactivemongo.api.collections.default.BSONCollection

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import com.sksamuel.scrimage.Image
import controllers.MainController._
import model.{SetRun, TestRun}
import org.joda.time.DateTime
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee.Enumerator
import reactivemongo.api.MongoDriver
import reactivemongo.api.gridfs.{DefaultFileToSave, GridFS}
import reactivemongo.api.gridfs.Implicits._
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONObjectID}

/**
 * Created by ipamer on 27/05/2014.
 */
object DbService {

  lazy val connection = new MongoDriver(Akka.system).connection(List("localhost"))
  lazy val db = connection.db("tstash")
  lazy val collectionSetRuns = db.collection[BSONCollection]("SetRuns")
  lazy val collectionTestRuns = db.collection[BSONCollection]("TestRuns")
  lazy val gfs = GridFS(db, "Screenshots")

  def insertTestRun(testRun: TestRun): (Future[SetRun], Future[TestRun]) = {

    val setRunFuture = findSetRun(SetRun(setName = testRun.setName.get, setDate = testRun.setDate)).map {
      case Some(sr) => sr
      case None => {
        val sr = SetRun(id = Some(BSONObjectID.generate), setName = testRun.setName.get, setDate = testRun.setDate, result = testRun.testResult)
        collectionSetRuns.insert(sr)
        sr
      }
    }

    val testRunFuture = setRunFuture.flatMap { sr =>
      findTestRun(testRun, sr.id.get).map {
        case Some(tr) => tr
        case None => {
          val tr = testRun.copy(id = Some(BSONObjectID.generate), setId = sr.id)
          collectionTestRuns.insert(tr).recover({ case x => println(x); x.printStackTrace()})
          tr
        }
      }
    }

    (setRunFuture, testRunFuture)
  }

  /*******************************
   * Insert Screen shot
   *******************************/

  def insertScreenshot(testRun: TestRun, setRun: SetRun, file: File) = {
    val fileToSave = DefaultFileToSave(file.getName)
    val resizedFile = Image(file).fitToWidth(600).write
    val enumerator = Enumerator(resizedFile)

    gfs.save(enumerator, fileToSave).map {
      case file =>
        val screenShotId = file.id.asInstanceOf[BSONObjectID]
        insertScreenShotToTestRun(testRun, setRun, screenShotId)
        Ok("Screenshot added.")
    } recover {
      case e =>
        Logger.error(e.toString)
        Logger.error(e.getCause.toString)
        InternalServerError("upload failed")
    }
  }

  private def insertScreenShotToTestRun(testRun: TestRun, setRun: SetRun, screenShotId: BSONObjectID): Unit = {
    findSetRun(setRun).map {
      case Some(sr) => {
        findTestRun(testRun, sr.id.get).map {
          case Some(tr) => {
            collectionTestRuns.update(BSONDocument("_id" -> tr.id.get), BSONDocument("$set" -> BSONDocument("screenShotId" -> screenShotId)))
              .recover { case e => new RuntimeException("Screen Shot insert failed.", e) }
          }
          case None => new RuntimeException("Test Run not found.")
        }
      }
      case None => new RuntimeException("Set Run not found.")
    }
  }

  private def findSetRun(setRun: SetRun): Future[Option[SetRun]] = {
    collectionSetRuns.find(BSONDocument("setName" -> setRun.setName, "setDate" -> setRun.setDate.map(date => BSONDateTime(date.getMillis)))).one[SetRun]
  }

  private def findTestRun(testRun: TestRun, setRunId: BSONObjectID): Future[Option[TestRun]] = {
    collectionTestRuns.find(BSONDocument("setId" -> setRunId, "testName" -> testRun.testName, "testDate" -> testRun.testDate.map(date => BSONDateTime(date.getMillis)))).one[TestRun]
  }

  /*******************************
   * Test case modifications
   *******************************/

  def setRunFailed(setAndTestFuture: (Future[SetRun], Future[TestRun]), error: String): Unit = {
    setAndTestFuture._1.map { sr =>
      collectionSetRuns.update(BSONDocument("_id" -> sr.id.get), BSONDocument("$set" -> BSONDocument("result" -> "FAILED")))
        .recover { case x => println(x); x.printStackTrace() }
    }
    setAndTestFuture._2.map { tr =>
      collectionTestRuns.update(BSONDocument("_id" -> tr.id.get), BSONDocument("$set" -> BSONDocument("testResult" -> "FAILED", "error" -> error)))
        .recover { case x => println(x); x.printStackTrace() }
    }
  }

  def addMessageToTestRun(testRunFuture: Future[TestRun], message: String): Unit = {
    testRunFuture.map { tr =>
      collectionTestRuns.update(BSONDocument("_id" -> tr.id.get), BSONDocument("$addToSet" -> BSONDocument("messages" -> message)))
        .recover { case x => println(x); x.printStackTrace() }
    }
  }

  /*******************************
   * Cleanup functions
   *******************************/

  def cleanupDB(daysToKeep: Int = 14): Unit = {
    Logger.info("Cleaning up DB...")
    collectionSetRuns.remove(BSONDocument("setDate" -> BSONDocument("$lt" -> BSONDateTime(DateTime.now().minusDays(daysToKeep).getMillis))))
    collectionTestRuns.remove(BSONDocument("testDate" -> BSONDocument("$lt" -> BSONDateTime(DateTime.now().minusDays(daysToKeep).getMillis))))
    gfs.find(BSONDocument("uploadDate" -> BSONDocument("$lt" -> BSONDateTime(DateTime.now().minusDays(daysToKeep).getMillis)))).collect[List]().map { list =>
      list.foreach { doc =>
        gfs.remove(doc.id.asInstanceOf[BSONObjectID])
      }
    }
  }

  /*******************************
   * Getters for html pages
   *******************************/

  def getAllSetRun(): Future[Map[String, List[SetRun]]] = {
    collectionSetRuns.find(BSONDocument()).sort(BSONDocument("setName" -> 1, "setDate" -> -1)).cursor[SetRun].collect[List]().map { setRunList =>
      setRunList.groupBy[String](setRun => setRun.setName)
    }
  }

  def getSetRunList(setName: String): Future[List[SetRun]] = {
    collectionSetRuns.find(BSONDocument("setName" -> setName)).sort(BSONDocument("setDate" -> -1)).cursor[SetRun].collect[List]()
  }

  def getAllTest(setId: BSONObjectID): Future[List[TestRun]] = {
    collectionTestRuns.find(BSONDocument("setId" -> setId)).sort(BSONDocument("testDate" -> -1)).cursor[TestRun].collect[List]()
  }

  def getTest(testId: BSONObjectID): Future[Option[TestRun]] = {
    collectionTestRuns.find(BSONDocument("_id" -> testId)).one[TestRun]
  }

  def getScreenShot(id: BSONObjectID) = {
    gfs.find(BSONDocument("_id" -> id))
  }

  def getAllTests(setName: String, setDate: DateTime): Future[List[TestRun]] = {
    val setRun = findSetRun(SetRun(setName = setName, setDate = Some(setDate)))
    setRun.flatMap {
      setRun2: Option[SetRun] =>
        val result = Future.sequence(setRun2.map(setRun3 => getAllTest(setRun3.id.get)))
        result.map(_.flatten.toList)
    }
  }

}
