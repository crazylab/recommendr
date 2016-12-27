package processing

import java.io.File
import java.nio.file.Paths

import akka.actor.Actor
import common.Constants
import controllers.Global
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel


object ReloadRequest
case class RecommendProducts(userId:Int, limit:Int)
case class RecommendSimilarProducts(productId:Int, limit:Int)
case class RecommendSimilarUsers(userId:Int, limit:Int)
case class RecommendUsers(productId:Int, limit:Int)
case class GetUserFactors(userId:Int)
case class ChangeWeightFactors(weights: Array[Double])
object GetProductFeatures
object GetWeightFactors

class ALSRecoActor extends Actor{
  var model: UserProductRecoModel = loadModel()

  def loadModel(): UserProductRecoModel = {
    if (new File(Constants.alsModelDir).exists) {
      val m: UserProductRecoModel = UserProductRecoModel(MatrixFactorizationModel.load(Global.ctx, Constants.alsModelDir))
      m.productFeatures.cache()
      m.userFeatures.cache()
      m
    } else null
  }

  def reloadModel(): Unit = {
    model = loadModel()
  }

  def changeWeightFactor(factor:Array[Double]): Unit = {
    model = model.withWeightFactor(factor)
    Global.ctx.parallelize(factor).saveAsTextFile("model/featureWeightFactors")
  }

  def recommendSimilarProducts(productId: Int, limit: Int): Seq[String] = {
    val productVector: Vector = Vectors.dense(model.productFeatures.lookup(productId).head)
    val sortedNearestProducts: Array[(Int, Double)] = model.productFeatures.map(x => (x._1, Vectors.sqdist(Vectors.dense(x._2), productVector))).collect().sortBy(_._2)
    sortedNearestProducts.take(limit).filter(_._1 != productId).map(_._1.toString)
  }

  def recommendSimilarUsers(userId: Int, limit: Int): Seq[String] = {
    val userVector: Vector = Vectors.dense(model.userFeatures.lookup(userId).head)
    val sortedNearestUsers: Array[(Int, Double)] = model.userFeatures.map(x => (x._1, Vectors.sqdist(Vectors.dense(x._2), userVector))).collect().sortBy(_._2)
    sortedNearestUsers.take(limit).filter(_._1 != userId).map(_._1.toString)
  }

  override def receive: Receive = {
    case ReloadRequest => reloadModel()
    case ChangeWeightFactors(weights) => changeWeightFactor(weights)
    case GetProductFeatures => sender ! model.productFeatures.collect()
    case GetWeightFactors => sender ! model.weightFactor
    case GetUserFactors(userId) => sender ! model.userFeatures.filter(_._1 == userId).collect()
    case RecommendProducts(userId, limit) => sender ! model.recommendProducts(userId, limit)
    case RecommendUsers(productId, limit) => sender ! model.recommendUsers(productId, limit)
    case RecommendSimilarProducts(productId, limit) => sender ! recommendSimilarProducts(productId, limit)
    case RecommendSimilarUsers(userId, limit) => sender ! recommendSimilarUsers(userId, limit)
  }
}
