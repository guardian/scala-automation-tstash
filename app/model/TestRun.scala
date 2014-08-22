package model

import org.joda.time.DateTime
import reactivemongo.bson._

case class TestRun(id: Option[BSONObjectID],
                   setId: Option[BSONObjectID],
                   testName: String,
                   testDate: Option[DateTime],
                   testResult: String,  // TODO: TestResult
                   error: Option[String],
                   messages: Option[List[String]])

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
        doc.getAs[List[String]]("messages"))
  }

  implicit object TestRunBSONWriter extends BSONDocumentWriter[TestRun] {
    def write(testCase: TestRun): BSONDocument =
      BSONDocument(
        "_id" -> testCase.id.getOrElse(BSONObjectID.generate),
        "setId" -> testCase.setId.getOrElse(BSONObjectID.generate),
        "testName" -> testCase.testName,
        "testDate" -> testCase.testDate.map(date => BSONDateTime(date.getMillis)),
        "testResult" -> testCase.testResult,
        "error" -> testCase.error,
        "messages" -> testCase.messages)
  }

}
