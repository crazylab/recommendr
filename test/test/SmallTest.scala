package test

import java.io.{FileOutputStream, PrintWriter}

import breeze.numerics.abs
import org.apache.spark.SparkContext
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD

import scala.collection.Map

object SmallTest {

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



    val ranks = List(5)//List(1,3,10,30,90,200,300)
    val lambdas:List[Double] = List(0.1)//List(0.00003, 0.0001,0.001, .01, 0.1,.3, 1, 3, 10)
    val numIters = List(20)// List(1,5,12,20,30)
    val alphas:List[Double] = List(1)//List(0.1, 0.3, 1, 3,10,30,90,120,200,1000) //List(0.001, 0.003, 0.01, 0.03, 0.1, 0.3)
    var bestModel: Option[MatrixFactorizationModel] = None
    var bestValidationRmse = (Double.MaxValue,0.0)
    var bestRank = 0
    var bestLambda = -1.0
    var bestNumIter = -1
    val positiveCase = sc.parallelize( for(u<- 1 until 6; p <- 1 until 12)  yield ((u,p),1))
    val failCase = sc.parallelize(List(
      ((1, 1),0),
      ((1, 5), 0),
      ((1, 9), 0),
      ((1, 10), 0),
      ((2, 4), 0),
      ((2, 6), 0),
      ((2, 8), 0),
      ((3, 6), 0),
      ((4, 6), 0),
      ((4, 7), 0),
      ((4, 9), 0),
      ((4, 10), 0),
      ((5, 6),0),
      ((5, 9),0),
      ((5, 10),0)
    ))

    val data = positiveCase.leftOuterJoin(failCase).map(x=>(x._1, if (x._2._2 == None) x._2._1 else x._2._2.get ))
//    val data = sc.parallelize(List("1,1", "1,2", "1,3", "2,3", "2,4", "3,3", "4,1", "4,2")).map(_.split(','))
//    val itemFreq:Map[Int, Int] = data.map(x=>x(1).toInt -> 1).reduceByKey(_+_).collectAsMap()
    val allTrx = data.count()
//    sc.broadcast(itemFreq)
//    val ratings:RDD[Rating] = data.map(x=>(x(0).trim.toInt, x(1).trim.toInt) -> 1).reduceByKey(_+_).map {
//      case ((user,item), count) => Rating(user, item, 1.0)
//    }
    val ratings:RDD[Rating] = data.map(x=> Rating(x._1._1, x._1._2, x._2))

//    val test: RDD[Rating] = ratings.sample(false, 0.0001, 1)
//    val train: RDD[Rating] = ratings.subtract(test)

    val f = new PrintWriter(new FileOutputStream("out.txt", true))
    for (rank <- ranks; lambda <- lambdas; numIter <- numIters; alpha <- alphas) {
      val model = ALS.trainImplicit(ratings, rank, numIter, lambda,8,alpha)
//      val validationRmse = computeRmse(model, test.union(failCase))
      val validationRmse = computeRmse(model, ratings)
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
    bestModel.get.save(sc,"../smodel")
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



