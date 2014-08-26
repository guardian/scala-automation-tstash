package service

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import com.sksamuel.scrimage.Image
import controllers.MainController._
import model.{SetRun, TestRun}
import model.SetRun.SetRunBSONWriter
import play.api.Logger
import play.api.Play.current
import play.api.libs.Files
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee.Enumerator
import play.api.mvc.MultipartFormData
import reactivemongo.api.MongoDriver
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api.gridfs.{DefaultFileToSave, GridFS}
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONObjectID}

/**
 * Created by ipamer on 27/05/2014.
 */
object DbService {

  lazy val connection = new MongoDriver(Akka.system).connection(List("localhost"))
  lazy val db = connection.db("tstash")
  lazy val gfs = GridFS(db, "tstash-screenshots")

  def insertScreenshot(photo: MultipartFormData.FilePart[Files.TemporaryFile]) = {
    val fileToSave = DefaultFileToSave(photo.filename, photo.contentType)
    val resizedFile = Image(photo.ref.file).fitToWidth(120).write
    val enumerator = Enumerator(resizedFile)
    // TODO: save file for the corresponding test run
    gfs.save(enumerator, fileToSave).map {
      case file =>
        val id = file.id.asInstanceOf[BSONObjectID]
        Ok("Screenshot added.")
    } recover {
      case e =>
        Logger.error(e.toString)
        InternalServerError("upload failed")
    }
  }

  def insertTestRun(setRun: SetRun, testRun: TestRun) = {

    val collectionSetRuns = db.collection[BSONCollection]("SetRuns")

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
      val collectionTestRuns = db.collection[BSONCollection]("TestRuns")
      collectionTestRuns.insert(tr).recover({ case x => println(x); x.printStackTrace() })
      tr
    }

    testRunFuture
  }

  def setTestRunFailed(testRunFuture: Future[TestRun], error: String): Unit = {
    testRunFuture.map { tr =>
      val collection = db.collection[BSONCollection]("TestRuns")
      collection.update(BSONDocument("_id" -> tr.id.get), tr.copy(testResult = "Failed", error = Some(error)), upsert = true)
        .recover { case x => println(x); x.printStackTrace() }
    }
  }

  def getSetRuns(): Future[List[SetRun]] = {
    val collection = db.collection[BSONCollection]("SetRuns")
    collection.find(BSONDocument()).sort(BSONDocument("setName" -> 1, "setDate" -> -1)).cursor[SetRun].collect[List]()
  }

  def getTests(setId: BSONObjectID): Future[List[TestRun]] = {
    val collection = db.collection[BSONCollection]("TestRuns")
    collection.find(BSONDocument("setId" -> setId)).sort(BSONDocument("testDate" -> -1)).cursor[TestRun].collect[List]()
  }

  def getTestMessages(testName: String, testDate: String, setName: String, setDate: String): Unit = {
    
  }

}
