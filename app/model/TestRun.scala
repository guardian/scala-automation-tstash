package model

import org.joda.time.DateTime
import play.api.libs.json.{Writes, Reads, JsPath}
import play.api.libs.functional.syntax._
import reactivemongo.bson._

case class TestRun(id: Option[BSONObjectID] = None,
                   setId: Option[BSONObjectID] = None,
                   setName: Option[String] = None,
                   setDate: Option[DateTime] = None,
                   testName: String,
                   testDate: Option[DateTime] = None,
                   testResult: String = "PASSED",
                   error: Option[String] = None,
                   messages: Option[List[String]] = None,
                   screenShotId: Option[BSONObjectID] = None)

object TestRun {

  implicit val testRunJsonReads: Reads[TestRun] = (
      (JsPath \ "testName").read[String] and
      (JsPath \ "testDate").read[String].map[DateTime](date => new DateTime(date.toLong)) and
      (JsPath \ "setName").read[String] and
      (JsPath \ "setDate").read[String].map[DateTime](date => new DateTime(date.toLong)) and
      (JsPath \ "message").readNullable[String] and
      (JsPath \ "error").readNullable[String]
    )((name, date, setName, setDate, message, error) =>
      TestRun(testName = name, testDate = Some(date), setName = Some(setName), setDate = Some(setDate),
        messages = message.map({List(_)}), error = error)
    )

  implicit val testRunJsonWrites: Writes[TestRun] = (
      (JsPath \ "testName").write[String] and
      (JsPath \ "testDate").write[DateTime]
    )(run => (run.testName, run.testDate.get))

  implicit object TestRunBSONReader extends BSONDocumentReader[TestRun] {
    def read(doc: BSONDocument): TestRun =
      TestRun(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[BSONObjectID]("setId"),
        None,
        None,
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
