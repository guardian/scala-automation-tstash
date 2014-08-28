package controllers

import scala.concurrent.ExecutionContext.Implicits.global

import model.{SetRun, TestRun}
import org.joda.time.DateTime
import play.Logger
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json._
import play.api.mvc._
import service.DbService

/**
 * Created by ipamer.
 */
object MessageController extends Controller {

  def report(testName: String, testDate: String, setName: String, setDate: String) = WebSocket.using[JsValue] { request =>

    val testRun = TestRun(None, None, testName, Option(new DateTime(testDate.toLong)), "Passed", None, None)
    val setRun = SetRun(None, setName, Option(new DateTime(setDate.toLong)))
    val testRunFuture = DbService.insertTestRun(setRun, testRun)

    val out = Enumerator(Json.parse("""{"message":"OK"}"""))

    val in = Iteratee.foreach[JsValue](json => {
      Logger.info(s"received: ($testName, $testDate, $setName, $setDate) ${json}")
      val message = (json \ "message").asOpt[String]
      val error = (json \ "error").asOpt[String]
      val timeStamp = (json \ "timeStamp").toString()

      message.map { str => DbService.addMessageToTestRun(testRunFuture, str) }
      error.map { str => DbService.setTestRunFailed(testRunFuture, str) } // test failed
    })

    (in, out)
  }

  //  implicit val testResultReads: Reads[TestResult] = EnumUtils.enumReads(TestResult)
//  val testInfoReads: Reads[TestInfo] = (
//    (JsPath \ "testName").read[String] and
//      (JsPath \ "testSet").read[String] and
//      (JsPath \ "testDuration").read[String] and
//      (JsPath \ "testResult").read[TestResult] and
//      (JsPath \ "error").readNullable[String]
//    )(TestInfo.apply _)

  //    def javascriptRoutes = Action { implicit request =>
  // this tracks back the javascript method call on server side.
  // E.g. index.js: jsRoutes.controllers.MessageController.getMessage().ajax({
  //        Ok(Routes.javascriptRouter("jsRoutes")(routes.javascript.MessageController.getMessage)).as(JAVASCRIPT)
  //    }

}
