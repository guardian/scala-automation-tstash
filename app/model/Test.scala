package model

import org.joda.time.DateTime
import reactivemongo.bson._

case class Test(testName: String,
                setName: String,
                updateDate: Option[DateTime])

object Test {

  implicit object TestBSONReader extends BSONDocumentReader[Test] {
    def read(doc: BSONDocument): Test =
      Test(
        doc.getAs[String]("testName").get,
        doc.getAs[String]("setName").get,
        doc.getAs[BSONDateTime]("updateDate").map(dt => new DateTime(dt.value)))
  }

  implicit object TestBSONWriter extends BSONDocumentWriter[Test] {
    def write(test: Test): BSONDocument =
      BSONDocument(
        "testName" -> test.testName,
        "setName" -> test.setName,
        "updateDate" -> test.updateDate.map(date => BSONDateTime(date.getMillis)))
  }

}
