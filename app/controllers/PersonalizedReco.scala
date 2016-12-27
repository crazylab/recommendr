package controllers

import java.nio.file.Paths
import java.util.concurrent.TimeUnit

import akka.pattern.ask
import akka.util.Timeout
import common.Constants
import models._
import org.apache.spark.mllib.recommendation.Rating
import play.api.Play
import play.api.db.slick._
import play.api.libs.json.Json._
import play.api.libs.json._
import play.api.mvc._
import processing._
import slick.driver.JdbcProfile

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object PersonalizedReco extends Controller with HasDatabaseConfig[JdbcProfile] {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)


  import driver.api._

  val products = TableQuery[Products]

  implicit val productFormat = Json.format[Product]
  //  implicit val tFormat = Json.format[Seq[(String, String)]]
  implicit val writer = new Writes[(String, String)] {
    def writes(t: (String, String)): JsValue = {
      Json.obj(t._1 -> t._2)
    }
  }

  implicit  val wRating = new Writes[Rating]{
    override def writes(o: Rating): JsValue = {
      Json.obj("user" -> o.user, "product" -> o.product, "rating" -> o.rating)
    }
  }

  def triggerUpdate() = Action {
    Paths.get(Constants.alsModelDir).toFile.mkdirs()
    Global.personalizedJobProcessingActor ! TriggerPersonalizedRecoJob()
    Ok("ok")
  }
  implicit val timeout = Timeout(5 , TimeUnit.MINUTES)

  def productRecommendation(userId:Option[Int], productId:Option[Int]) = Action.async {

    if(userId.isDefined) {
      val future: Future[Array[Rating]] = (Global.recommendationActor ? RecommendProducts(userId.get, 10)).asInstanceOf[Future[Array[Rating]]]
      future.map(x => Ok(Json.toJson(x)))
    } else {
      Future {Ok("not implemented")}
    }
  }

  def forProduct(productId:Int) = Action {
    Global.recommendationActor ! RecommendUsers(productId, 10)
    Ok("")
  }
}