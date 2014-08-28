import akka.actor.Props
import service.CleanupActor

import scala.concurrent.duration.DurationInt

import play.api.{Application, GlobalSettings}
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Created by ipamer on 28/08/2014.
 */
object Global extends GlobalSettings {

  override def onStart(app: Application): Unit = {
    val cleanupActor = Akka.system(app).actorOf(Props(new CleanupActor()))
    Akka.system(app).scheduler.schedule(0 seconds, 1 day, cleanupActor, "cleanupActor")
  }

}
