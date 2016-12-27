package processing

import java.io.File
import java.nio.file.{Path, Files, Paths}

import breeze.numerics._
import common.Constants
import controllers.{Global, MySparkContext}
import models.OfflineBuyEvent
import org.apache.commons.io.FileUtils
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD

class ALSReco {
  val rank: Int = 89

  val iterations: Int = 100

  val lambda: Double = 0.1

  val blocks: Int = 8

  val alpha: Double = 1

  def addUserRatings(ctx: MySparkContext, purchases : RDD[OfflineBuyEvent]) = {
    val allTrx:Long = purchases.count()
    val distinctUserProduct: RDD[(Int, Int)] = purchases.map(purchase => purchase.getUserId -> purchase.getProductId).distinct()
    distinctUserProduct.persist()
    val numberOfUsers = distinctUserProduct.map(x=>x._1).distinct().count()
    val itemNoveltyScore = distinctUserProduct.map(_._2->1).reduceByKey(_+_).map(x=>x._1->(1-x._2.toDouble / numberOfUsers)).collectAsMap()
    val itemFreq=purchases.map(purchase => purchase.getProductId -> 1).reduceByKey(_+_).collectAsMap()
    val userFreq=purchases.map(purchase => purchase.getUserId -> 1).reduceByKey(_+_).collectAsMap()
    val ratingsRDD = purchases.map(purchase => (purchase.asTuple, 1)).reduceByKey(_+_).map {
      case ((user,item), count) => Rating(user, item,
      {
        val otherRecurrenceFactor: Double = itemFreq.getOrElse(item, 1) * 1.0 / allTrx
        val otherRating = if(otherRecurrenceFactor <= 0.01) 1.0 else math.exp(-50*otherRecurrenceFactor+0.08)

        val userRecurrenceFactor: Double = count * 1.0 / userFreq.getOrElse(user, 1)
        val userRating  = if(userRecurrenceFactor <= 0.2) 1.0
        else if(userRecurrenceFactor > 0.2 && otherRecurrenceFactor <=0.01)
          1.0
        else
          -math.log(2*userRecurrenceFactor - 0.22) + 0.25
        val itemNovelty = itemNoveltyScore.getOrElse(item,1.0)-0.7
        otherRating * userRating * itemNovelty
      })
    }

    val model = ALS.trainImplicit(ratingsRDD, rank, iterations, lambda, blocks, alpha)
    val alsModelPath: Path = Paths.get(Constants.alsModelDir)
    if (Files.exists(alsModelPath))
      FileUtils.deleteDirectory(alsModelPath.toFile)
    model.save(ctx, Constants.alsModelDir)
    Global.recommendationActor ! ReloadRequest
  }

  def computeRmse(model: MatrixFactorizationModel, ratings: RDD[Rating]) = {
    val usersProducts = ratings.map { case Rating(user, product, rate) =>
      (user, product)
    }
    val predictions =
      model.predict(usersProducts).map { case Rating(user, product, rate) =>
        ((user, product), rate)
      }
    val ratesAndPreds = ratings.map { case Rating(user, product, rate) =>
      ((user, product), rate)
    }.join(predictions)

    val MSE = ratesAndPreds.map { case ((user, product), (r1, r2)) =>
      val err = (r1 - r2)
      err * err
    }.mean()
    sqrt(MSE)
  }
}

