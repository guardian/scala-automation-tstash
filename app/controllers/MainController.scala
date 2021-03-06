package controllers

import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.mvc._
import play.modules.reactivemongo.MongoController
import reactivemongo.bson.BSONObjectID
import service.DbService

/**
 * Created by ipamer.
 */
object MainController extends Controller with MongoController {

  def index = Action.async {
    DbService.getAllSetRun().map { x => Ok(views.html.index(x)) }
  }

  def project(name: String) = Action.async {
    DbService.getSetRunList(name).map { x => Ok(views.html.project(name, x)) }
  }

  def set(setId: String) = Action.async {
    DbService.getAllTest(BSONObjectID(setId)).map { x => Ok(views.html.set(x)) }
  }

  def setLookup(setName: String, setDate: String) = Action.async {
    DbService.getAllTests(setName, new DateTime(setDate.toLong)).map { x => Ok(views.html.set(x)) }
  }

  def test(id: String) = Action.async {
    DbService.getTest(BSONObjectID(id)).map {
      case Some(x) => Ok(views.html.test(x))
      case None => Redirect(routes.MainController.index())
    }
  }

  def screenShot(id: String) = Action.async {
    serve(DbService.gfs, DbService.getScreenShot(BSONObjectID(id)))
  }

}
