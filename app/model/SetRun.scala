package model

import org.joda.time.DateTime
import reactivemongo.api.indexes.{IndexType, Index}
import reactivemongo.bson._

case class SetRun(id: Option[BSONObjectID] = None,
                  setName: String,
                  setDate: Option[DateTime] = None,
                  result: String = "PASSED")

object SetRun {

  // TODO: apply somewhere -> collection.indexesManager.ensure(SetRun.index)
  val index = Index(List("setDate"-> IndexType.Ascending, "setName" -> IndexType.Ascending), unique = true)

  implicit object SetRunBSONReader extends BSONDocumentReader[SetRun] {
    def read(doc: BSONDocument): SetRun =
      SetRun(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[String]("setName").get,
        doc.getAs[BSONDateTime]("setDate").map(dt => new DateTime(dt.value)),
        doc.getAs[String]("result").get)
  }

  implicit object SetRunBSONWriter extends BSONDocumentWriter[SetRun] {
    def write(setRun: SetRun): BSONDocument =
      BSONDocument(
        "_id" -> setRun.id.getOrElse(BSONObjectID.generate),
        "setName" -> setRun.setName,
        "setDate" -> setRun.setDate.map(date => BSONDateTime(date.getMillis)),
        "result" -> setRun.result)
  }

}
