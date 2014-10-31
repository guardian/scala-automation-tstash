package controllers

import model.{SetRun, TestRun}
import org.joda.time.DateTime
import play.Logger
import play.api.libs.json._
import play.api.mvc._
import service.DbService

/**
 * Created by ipamer.
 */
object MessageController extends Controller {

  def report() = Action(BodyParsers.parse.json) { request =>
    val result = request.body.validate[TestRun]
    result.fold(
      errors => {
        BadRequest(Json.obj("status" ->"KO", "message" -> JsError.toFlatJson(errors)))
      },
      testRun => {
        Logger.info("Adding message: " + testRun)

        val setAndTestFuture = DbService.insertTestRun(testRun)

        testRun.messages.map { messages => DbService.addMessageToTestRun(setAndTestFuture._2, messages(0)) }
        testRun.error.map { DbService.setRunFailed(setAndTestFuture, _) } // test failed

        Ok(Json.obj("status" ->"OK", "message" -> ("OK") ))
      }
    )
  }

  def screenShotUpload(testName: String, testDate: String, setName: String, setDate: String) = Action.async(parse.temporaryFile) { request =>
    Logger.info(s"received: ($testName, $testDate, $setName, $setDate) Screen shot received.")
    val testRun = TestRun(testName = testName, testDate = Option(new DateTime(testDate.toLong)))
    val setRun = SetRun(setName = setName, setDate = Option(new DateTime(setDate.toLong)))
    DbService.insertScreenshot(testRun, setRun, request.body.file)
  }

  //    def javascriptRoutes = Action { implicit request =>
  // this tracks back the javascript method call on server side.
  // E.g. index.js: jsRoutes.controllers.MessageController.getMessage().ajax({
  //        Ok(Routes.javascriptRouter("jsRoutes")(routes.javascript.MessageController.getMessage)).as(JAVASCRIPT)
  //    }

}
