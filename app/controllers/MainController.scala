package controllers

import play.modules.reactivemongo.MongoController

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.mvc._
import reactivemongo.bson.BSONObjectID
import service.DbService

/**
 * Created by ipamer.
 */
object MainController extends Controller with MongoController {

  def index = Action.async {
    DbService.getAllSetRun().map { x => Ok(views.html.index(x)) }
  }

  def set(setId: String) = Action.async {
    DbService.getAllTest(BSONObjectID(setId)).map { x => Ok(views.html.set(x)) }
  }

  def test(id: String) = Action.async {
    DbService.getTest(BSONObjectID(id)).map { x => Ok(views.html.test(x)) }
  }

  def screenShot(id: String) = Action.async {
    serve(DbService.gfs, DbService.getScreenShot(BSONObjectID(id)))
  }

}
