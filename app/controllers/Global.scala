package controllers


import akka.actor.{ActorRef, Props}
import org.apache.spark.SparkContext
import play.api._
import play.api.libs.concurrent.Akka
import processing.{DataImportActor, ALSRecoActor, PersonalizedJobProcessingActor, NonPersonalizedJobProcessingActor}


class MySparkContext () extends SparkContext("local[*]", "Spark shell") {

  override def stop(): Unit = {
    super.stop()
  }


}

object Global extends GlobalSettings {

  var ctx:MySparkContext = null
  var jobProcessingActor:ActorRef = null
  var importProcessingActor:ActorRef = null
  var personalizedJobProcessingActor:ActorRef = null
  var recommendationActor:ActorRef = null

  override def onStart(application: Application) {
    implicit val app = application
    ctx = new MySparkContext()
    Logger.info("Application has started")
    jobProcessingActor = Akka.system.actorOf(Props[NonPersonalizedJobProcessingActor], name = "jobProcessingActor")
    personalizedJobProcessingActor = Akka.system.actorOf(Props[PersonalizedJobProcessingActor], name = "personalizedJobProcessingActor")
    recommendationActor = Akka.system.actorOf(Props[ALSRecoActor], name = "recommendationActor")
    importProcessingActor = Akka.system.actorOf(Props[DataImportActor], name = "dataImportActor")


  }

  override def onStop(app: Application) {
    ctx.stop()
    Logger.info("Application shutdown...")
  }

}