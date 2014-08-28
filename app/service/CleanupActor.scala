package service

import akka.actor.Actor

/**
 * Created by ipamer on 28/08/2014.
 */
class CleanupActor extends Actor {

  def receive = {
    case _ => {
      DbService.cleanupDB()
    }
  }

}
