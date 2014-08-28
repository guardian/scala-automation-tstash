package model

import org.joda.time.DateTime
import reactivemongo.bson._

case class TestRun(id: Option[BSONObjectID],
                   setId: Option[BSONObjectID],
                   testName: String,
                   testDate: Option[DateTime],
                   testResult: String,
                   error: Option[String],
                   messages: Option[List[String]],
                   screenShotId: Option[BSONObjectID])

object TestRun {

  implicit object TestRunBSONReader extends BSONDocumentReader[TestRun] {
    def read(doc: BSONDocument): TestRun =
      TestRun(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[BSONObjectID]("setId"),
        doc.getAs[String]("testName").get,
        doc.getAs[BSONDateTime]("testDate").map(dt => new DateTime(dt.value)),
        doc.getAs[String]("testResult").get,
        doc.getAs[String]("error"),
        doc.getAs[List[String]]("messages"),
        doc.getAs[BSONObjectID]("screenShotId"))
  }

  implicit object TestRunBSONWriter extends BSONDocumentWriter[TestRun] {
    def write(testRun: TestRun): BSONDocument =
      BSONDocument(
        "_id" -> testRun.id.getOrElse(BSONObjectID.generate),
        "setId" -> testRun.setId.getOrElse(BSONObjectID.generate),
        "testName" -> testRun.testName,
        "testDate" -> testRun.testDate.map(date => BSONDateTime(date.getMillis)),
        "testResult" -> testRun.testResult,
        "error" -> testRun.error,
        "messages" -> testRun.messages)
  }

}
