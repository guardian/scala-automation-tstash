package controllers

import play.Logger

import scala.concurrent.ExecutionContext.Implicits.global

import model.{SetRun, TestRun}
import org.joda.time.DateTime
import play.api.mvc._
import reactivemongo.bson.BSONObjectID
import service.DbService

/**
 * Created by ipamer.
 */
object MainController extends Controller {

    def screenShotUpload(testName: String, testDate: String, setName: String, setDate: String) = Action.async(parse.temporaryFile) { request =>
      Logger.info(s"received: ($testName, $testDate, $setName, $setDate) Screen shot received.")
      val testRun = TestRun(None, None, testName, Option(new DateTime(testDate.toLong)), "Passed", None, None)
      val setRun = SetRun(None, setName, Option(new DateTime(setDate.toLong)))
      DbService.insertScreenshot(testRun, setRun, request.body.file)
    }

    def index = Action.async {
      // TODO: send refined structures to the HTML - grouped setRuns, passed or failed
      DbService.getAllSetRun().map { x => Ok(views.html.index(x)) }
    }
    
    def set(setId: String) = Action.async {
      DbService.getAllTest(BSONObjectID.parse(setId).get).map { x => Ok(views.html.set(x)) }
    }

    def test(testName: String, testDate: String, setName: String, setDate: String) = Action {
//      Ok(views.html.test(DbService.getTestMessages(testName, testDate, setName, setDate)))
      Ok("TEST")
    }

}
