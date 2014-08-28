package service

import java.io.File

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import com.sksamuel.scrimage.Image
import controllers.MainController._
import model.{SetRun, TestRun}
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee.Enumerator
import reactivemongo.api.MongoDriver
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.gridfs.{DefaultFileToSave, GridFS}
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONObjectID}
import reactivemongo.api.gridfs.Implicits._

/**
 * Created by ipamer on 27/05/2014.
 */
object DbService {

  lazy val connection = new MongoDriver(Akka.system).connection(List("localhost"))
  lazy val db = connection.db("tstash")
  lazy val collectionSetRuns = db.collection[BSONCollection]("SetRuns")
  lazy val collectionTestRuns = db.collection[BSONCollection]("TestRuns")
  lazy val gfs = GridFS(db, "Screenshots")

  def insertScreenshot(testRun: TestRun, setRun: SetRun, file: File) = {
    val fileToSave = DefaultFileToSave(file.getName)
    val resizedFile = Image(file).fitToWidth(120).write
    val enumerator = Enumerator(resizedFile)

    gfs.save(enumerator, fileToSave).map {
      case file =>
        val screenShotId = file.id.asInstanceOf[BSONObjectID]
        insertScreenShotToTestRun(testRun, setRun, screenShotId)
        Ok("Screenshot added.")
    } recover {
      case e =>
        Logger.error(e.toString)
        InternalServerError("upload failed")
    }
  }

  private def insertScreenShotToTestRun(testRun: TestRun, setRun: SetRun, screenShotId: BSONObjectID): Unit = {
    findTest(setRun, testRun).map(_.map({ tr =>
        collectionTestRuns.update(BSONDocument("_id" -> tr.id.get), BSONDocument("$set" -> BSONDocument("screenShotId" -> screenShotId)))
          .recover { case x => println(x); x.printStackTrace() }
    }))
  }

  private def findTest(setRun: SetRun, testRun: TestRun): Future[Future[TestRun]] = {
    findSetRun(setRun).map {
      case Some(sr) => {
        findTestRun(testRun, sr.id.get).map {
          case Some(tr) => tr
        }
      }
//      case None => Failure(new RuntimeException("Test Run not found."))
    }
  }

  private def findSetRun(setRun: SetRun): Future[Option[SetRun]] = {
    collectionSetRuns.find(BSONDocument("setName" -> setRun.setName, "setDate" -> setRun.setDate.map(date => BSONDateTime(date.getMillis)))).one[SetRun]
  }

  private def findTestRun(testRun: TestRun, setRunId: BSONObjectID): Future[Option[TestRun]] = {
    collectionTestRuns.find(BSONDocument("setId" -> setRunId, "testName" -> testRun.testName, "testDate" -> testRun.testDate.map(date => BSONDateTime(date.getMillis)))).one[TestRun]
  }





  def insertTestRun(setRun: SetRun, testRun: TestRun) = {

    val setRunFuture = collectionSetRuns.find(BSONDocument("setName" -> setRun.setName, "setDate" -> setRun.setDate.map(date => BSONDateTime(date.getMillis)))).one[SetRun].map {
      case Some(sr) => sr
      case None => {
        val sr = setRun.copy(id = Some(BSONObjectID.generate))
        collectionSetRuns.insert(sr)
        sr
      }
    }

    // insert TestRun
    val testRunFuture = setRunFuture.map { sr =>
      val tr = testRun.copy(id = Some(BSONObjectID.generate), setId = sr.id)
      collectionTestRuns.insert(tr).recover({ case x => println(x); x.printStackTrace() })
      tr
    }

    testRunFuture
  }





  def setTestRunFailed(testRunFuture: Future[TestRun], error: String): Unit = {
    testRunFuture.map { tr =>
      collectionTestRuns.update(BSONDocument("_id" -> tr.id.get), BSONDocument("$set" -> BSONDocument("testResult" -> "Failed", "error" -> error)))
        .recover { case x => println(x); x.printStackTrace() }
    }
  }





  def getAllSetRun(): Future[Map[String, List[SetRun]]] = {
    collectionSetRuns.find(BSONDocument()).sort(BSONDocument("setName" -> 1, "setDate" -> -1)).cursor[SetRun].collect[List]().map { setRunList =>
      setRunList.groupBy[String](setRun => setRun.setName)
    }
  }

  def getAllTest(setId: BSONObjectID): Future[List[TestRun]] = {
    collectionTestRuns.find(BSONDocument("setId" -> setId)).sort(BSONDocument("testDate" -> -1)).cursor[TestRun].collect[List]()
  }

  def getAllTestMessage(testName: String, testDate: String, setName: String, setDate: String): Unit = {
    
  }

}
