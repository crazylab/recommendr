package processing

import java.io.{FileWriter, PrintWriter}
import java.nio.file.{Files, Paths}

import akka.actor.Actor
import common.Constants
import controllers.Global
import models.OfflineBuyEvent
import org.apache.spark.rdd.RDD

case class TriggerPersonalizedRecoJob()
case class UserRatingList(userId : String, tx : Array[String])

class PersonalizedJobProcessingActor extends Actor{

  def updateScores(): Unit = {

    val purchases: RDD[OfflineBuyEvent] = Global.ctx.textFile(Constants.actionLogFileLocation).map(x => x.split(",")).filter(x => x(0) == Constants.offlineBuy).map(x => OfflineBuyEvent(x(1), x(3)))

    val alsReco = new ALSReco()
    alsReco.addUserRatings(Global.ctx, purchases)
    Global.recommendationActor ! ReloadRequest
  }
  
  override def receive: Receive = {
    case TriggerPersonalizedRecoJob() => updateScores()
  }
}
