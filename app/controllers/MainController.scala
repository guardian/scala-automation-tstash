package controllers

import play.api.mvc._
import reactivemongo.bson.BSONObjectID
import service.DbService
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by ipamer.
 */
object MainController extends Controller {
    
    def index = Action.async {
      // TODO: send refined structures to the HTML - grouped setRuns, passed or failed
      DbService.getSetRuns().map { x => Ok(views.html.index(x)) }
    }
    
    def set(setId: String) = Action.async {
      DbService.getTests(BSONObjectID.parse(setId).get).map { x => Ok(views.html.set(x)) }
    }

    def test(testName: String, testDate: String, setName: String, setDate: String) = Action {
//      Ok(views.html.test(DbService.getTestMessages(testName, testDate, setName, setDate)))
      Ok("TEST")
    }

}
