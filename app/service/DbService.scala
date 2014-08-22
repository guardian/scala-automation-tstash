package service

import org.joda.time.DateTime

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global

import model.{Test, TestRun, SetRun}
import model.SetRun.SetRunBSONWriter
import play.api.Play.current
import play.api.libs.concurrent.Akka
import reactivemongo.api.MongoDriver
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONDocument, BSONDateTime, BSONObjectID}

/**
 * Created by ipamer on 27/05/2014.
 */
object DbService {

  val connection = new MongoDriver(Akka.system).connection(List("localhost"))

  def db() = {
    connection.db("tstash")
  }

  // TODO: close mongo connections?

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

    // track test - TODO: might not needed
//    val collectionTests = db.collection[BSONCollection]("Tests")
//    val test = Test(testRun.testName, setRun.setName, Some(DateTime.now))
//    collectionTests.update(BSONDocument("testName" -> testRun.testName, "setName" -> setRun.setName), test, upsert = true).recover { case x => println(x); x.printStackTrace() }

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
