package test

import java.io.{PrintWriter, FileOutputStream}
import java.util.logging

import akka.event.slf4j
import breeze.numerics.{log, abs}
import ch.qos.logback.classic
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import org.slf4j.LoggerFactory

import scala.collection.Map

object LTest {
/*
  def test(sc: SparkContext, lambda: Double, alpha: Double, numIterations: Int): Double = {
    val data = sc.textFile("als_data_from_transaction.csv", 8)
    //data.foreach(println)
    val ratings = data.map(_.split(',') match { case Array(user, item) =>
      Rating(user.toInt, item.toInt, 1.0)
    })
    // Build the recommendation model using ALS
    val rank = 10
    //val numIterations = 10
    val alpha = 0.01
    val lambda = 0.01
    val model = ALS.trainImplicit(ratings, rank, numIterations, lambda, alpha)

    // Evaluate the model on rating data
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
    println("Mean Squared Error = " + MSE)
    MSE
  }*/
  // Save and load model
  //model.save(sc, "myModelPath")
  //val sameModel = MatrixFactorizationModel.load(sc, "myModelPath")
  //find regularization
  //alpha = 0.1 is optimal
  //lambda = 0.00003 is optimal



  def computeRmse(model: MatrixFactorizationModel, ratings: RDD[Rating]) = {

    val usersProducts = ratings.map { case Rating(user, product, rate) =>
      (user, product)
    }
    val predictions =
      model.predict(usersProducts).map { case Rating(user, product, rate) =>
        ((user, product), rate)
      }
    val meanRating = predictions.map(_._2).mean()
    val ratesAndPreds = ratings.map { case Rating(user, product, rate) =>
      ((user, product), rate)
    }.join(predictions)

    val MSE = ratesAndPreds.map { case ((user, product), (r1, r2)) =>
      println(user + "," + product + "," + r1 + "," + r2 + "," + (r1-r2))
      val err = (r1 - r2)


//      Math.exp(abs(err))-1
      1 + Math.tanh(7 * abs(err)-3)
    }.mean()

    val missed = ratesAndPreds.map(x=> if(x._2._1 == 1 && x._2._2 < meanRating || x._2._1 == 0 && x._2._2 > meanRating) 1 else 0).sum()
    println("validation for " + ratings.count())

    (MSE, 100.0*missed/ratings.count())
  }
  def main(arg:Array[String]) :Unit={

    val sc = new SparkContext("local[*]", "name")

//    sc.addJar("some.jar")
//    sc.addFile("user_ratings.csv.bk")

    //val  a = (1 to 10).map(x => test(sc,0.01, 0.0001*3*x, 10))
//    val logger:classic.Logger =  Logger.get.asInstanceOf[classic.Logger]
//    logger.setLevel(classic.Level.OFF)


    val ranks = List(21)//List(1,3,10,30,90,200,300)
    val lambdas:List[Double] = List(0.1)//List(0.00003, 0.0001,0.001, .01, 0.1,.3, 1, 3, 10)
    val numIters = List(30)// List(1,5,12,20,30)
    val alphas:List[Double] = List(1)//List(0.1, 0.3, 1, 3,10,30,90,120,200,1000) //List(0.001, 0.003, 0.01, 0.03, 0.1, 0.3)
    var bestModel: Option[MatrixFactorizationModel] = None
    var bestValidationRmse = (Double.MaxValue,0.0)
    var bestRank = 0
    var bestLambda = -1.0
    var bestNumIter = -1
    val data = sc.textFile("../data/user_ratings.csv", 8).map(_.split(','))
    val itemFreq:Map[Int, Int] = data.map(x=>x(1).toInt -> 1).reduceByKey(_+_).collectAsMap()
    val allTrx = data.count()
    sc.broadcast(itemFreq)
    val ratings:RDD[Rating] = data.map(x=>(x(0).trim.toInt, x(1).trim.toInt) -> 1).reduceByKey(_+_).map {
      case ((user,item), count) => Rating(user, item, 1.0/Math.pow(10000, 55.0*itemFreq.getOrElse(item,1)/allTrx))
    }

    val test: RDD[Rating] = ratings.sample(false, 0.0001, 1)
    val train: RDD[Rating] = ratings.subtract(test)
    val failCase = sc.parallelize(List(
      Rating(400, 996218, 0),
      Rating(2125,1111783, 0),
      Rating(2125,12263913, 0),
      Rating(2125,6534469, 0),
      Rating(458 , 13008176 , 0),
      Rating(897 , 16734247 , 0),
      Rating(397 , 15925409 , 0),
      Rating(641 , 12487547 , 0),
      Rating(1831, 7440953  , 0),
      Rating(627 , 15561535 , 0),
      Rating(115 , 10254357 , 0),
      Rating(2393, 1054972  , 0),
      Rating(349 , 9364767  , 0),
      Rating(2224, 16766163 , 0),
      Rating(2040 , 995055, 0),
      Rating(2433 , 999076, 0),
      Rating(184 , 995303, 0),
      Rating(313 , 994637, 0),
      Rating(1415 , 981837, 0),
      Rating(1538 , 999714, 0),
      Rating(226 , 993838, 0),
      Rating(2220 , 994163, 0),
      Rating(760 , 999142, 0),
      Rating(1992 , 9705473, 0),
      Rating(1933 , 975644, 0),
      Rating(1320 , 981760, 0),
      Rating(962 , 990440, 0),
      Rating(1090 , 998328, 0),
      Rating(522 , 999973, 0),
      Rating(1051 , 993671, 0),
      Rating(1491 , 999779, 0),
      Rating(2216 , 999953, 0),
      Rating(976 , 994980, 0),
      Rating(1357 , 990495, 0),
      Rating(267 , 977499, 0),
      Rating(2311 , 992985, 0),
      Rating(1497 , 983584, 0),
      Rating(589 , 999332, 0),
      Rating(1906 , 9836695, 0),
      Rating(1211 , 997112, 0),
      Rating(372 , 970119, 0),
      Rating(100   , 992951,0),
      Rating(123, 999953, 0),
      Rating(345, 997112, 0),
      Rating(1157, 993838, 0),
      Rating(246,9858887, 0),
      Rating(2056 , 9835469,0),
      Rating(1941 , 997200,0),
      Rating(1603 , 996582,0),
      Rating(1980 , 693056,0),
      Rating(2133 , 989375,0),
      Rating(182 , 9858887,0),
      Rating(572 , 993988,0),
      Rating(1087 , 995722,0),
      Rating(1305 , 999074,0),
      Rating(1727 , 993913,0),
      Rating(1647 , 998112,0),
      Rating(192 , 962822,0),
      Rating(1710 , 995276,0),
      Rating(1043 , 9962526,0),
      Rating(511 , 998678,0),
      Rating(1416 , 985361,0),
      Rating(276,9858887, 0)
    ))
    val f = new PrintWriter(new FileOutputStream("out.txt", true))
    for (rank <- ranks; lambda <- lambdas; numIter <- numIters; alpha <- alphas) {
      val model = ALS.trainImplicit(train, rank, numIter, lambda,8,alpha)
      val validationRmse = computeRmse(model, test.union(failCase))
      f.println(""+ validationRmse + "," + rank + ", " + lambda + "," + numIter + ", alpha=" + alpha + "")
      f.flush()
      if (validationRmse._1 < bestValidationRmse._1) {
        bestModel = Some(model)
        bestValidationRmse = validationRmse
        bestRank = rank
        bestLambda = lambda
        bestNumIter = numIter
      }
    }
    f.close()
    println("The best model was trained with rank = " + bestRank + " and lambda = " + bestLambda
    + ", and numIter = " + bestNumIter + ", rmse = " + bestValidationRmse + ".")
    bestModel.get.save(sc,"bestModel")
  }

  /*def main2(a:Array[String]) = {
      val sc = new MySparkCxontext()
      val data = sc.parallelize(List("1,1", "2,2"))
      //data.foreach(println)
      val ratings = data.map(_.split(',') match { case Array(user, item) =>
        Rating(user.toInt, item.toInt, 1.0)
      })
      // Build the recommendation model using ALS
      val rank = 10
      val numIterations = 10
      val alpha = 0.01
      val lambda = 0.01
      val model = ALS.trainImplicit(ratings, rank, numIterations, lambda, alpha)

      // Evaluate the model on rating data
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
      println("Mean Squared Error = " + MSE)


  }*/
}



