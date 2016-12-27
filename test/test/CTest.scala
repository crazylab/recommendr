package test

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.network.netty.SparkTransportConf
import org.apache.spark.rdd.RDD

object CTest {
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
    val ratesAndPreds = ratings.map { case Rating(user, product, rate) =>
      ((user, product), rate)
    }.join(predictions)

    val MSE = ratesAndPreds.map { case ((user, product), (r1, r2)) =>
      val err = (r1 - r2)
      err
    }.mean()
    MSE
  }
  def main(arg:Array[String]) :Unit={
    val c:SparkConf = new SparkConf()
    c.set("spark.executor.memory","20G")
    c.set("spark.driver.memory","2G")
    c.set("spark.akka.frameSize", "1000")
    val sc = new SparkContext("spark://yottabyte01.thoughtworks.com:7077", "name", conf = c)

//    sc.addJar("some.jar")
//    sc.addFile("user_ratings.csv.bk")

    //val  a = (1 to 10).map(x => test(sc,0.01, 0.0001*3*x, 10))

    val ranks = List(300)//List(1,3,10,30,90,200,500)
    val lambdas:List[Double] = List(0)//List(0.00003, 0.0001,0.001, .01, 0.1,.3, 1, 3, 10)
    val numIters = List(20)// List(1,5,12,20,30)
    val alphas:List[Double] = List(3.0) //List(0.1, 0.3, 1, 3) //List(0.001, 0.003, 0.01, 0.03, 0.1, 0.3)
    var bestModel: Option[MatrixFactorizationModel] = None
    var bestValidationRmse = Double.MaxValue
    var bestRank = 0
    var bestLambda = -1.0
    var bestNumIter = -1
    val data = sc.textFile("hdfs://10.10.5.97:8020//user/vaibhavk/user_ratings.csv", 50)
    val ratings:RDD[Rating] = data.map(_.split(',') match { case Array(user, item) =>
      Rating(user.trim.toInt, item.trim.toInt, 1.0)
    }).distinct()

//    val test: RDD[Rating] = ratings.sample(false, 0.2, 1)
//    val train: RDD[Rating] = ratings.subtract(test)

//    val f = new PrintWriter(new FileOutputStream("out.txt", true))
    for (rank <- ranks; lambda <- lambdas; numIter <- numIters; alpha <- alphas) {
      val model = ALS.trainImplicit(ratings, rank, numIter, lambda,8,alpha)
      val validationRmse = computeRmse(model, ratings)
//      f.println(""+ validationRmse + "," + rank + ", " + lambda + "," + numIter + ", alpha=" + alpha + "")
//      f.flush()
      if (validationRmse < bestValidationRmse) {
        bestModel = Some(model)
        bestValidationRmse = validationRmse
        bestRank = rank
        bestLambda = lambda
        bestNumIter = numIter
      }
    }
//    f.close()
    bestModel.get.save(sc,"bestModel")
    println("The best model was trained with rank = " + bestRank + " and lambda = " + bestLambda
      + ", and numIter = " + bestNumIter + ", rmse = " + bestValidationRmse + ".")
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
