package org.apache.spark.mllib

import controllers.MySparkContext
import org.apache.spark.mllib.clustering.{Helper, VectorWithNorm, KMeans}
import org.apache.spark.mllib.linalg.{Vectors, Vector}
import org.apache.spark.mllib.recommendation.MatrixFactorizationModel
import org.apache.spark.rdd.RDD

object KmeanTest {
  def main(a:Array[String]): Unit ={

    val sc = new MySparkContext()
    val numIterations = 50
    val m=MatrixFactorizationModel.load(sc, "data/test001/personalized/als/model")
    val p = sc.textFile("/tmp/tbl1.csv").map(x => x.split(",")).map(x=>x(0)->x.mkString(",")).collectAsMap

    val parsedData = m.productFeatures.map(x=>Vectors.dense(x._2)).cache()

//    val clusters = KMeans.train(parsedData, 20, numIterations)

    val dataPts: RDD[(Int, Array[Double])] = m.productFeatures.filter(x => x._1 != 968215)
    val dataIn: RDD[Vector] = dataPts.map(x => Vectors.dense(x._2))
    val points = dataIn.map(x => Helper.getVN(x)).toArray()

    val p1 = Vectors.dense(m.productFeatures.filter(x => x._1 == 968215).take(1)(0)._2)

//    val zipped = clusters.predict(m.productFeatures.map(x=>Vectors.dense(x._2))).zip(m.productFeatures.map(x => x._1))
//    val grouped= zipped.groupByKey().map(x => (x._1, x._2.toArray))
//    grouped.map(x=> x._1 -> x._2.length).collect
//    val g =grouped.collect.filter(_._1 != 1)
//    grouped.collect.filter(_._1 == 7).flatMap(x => x._2).map(x => p(x.toString)).foreach(println)
//
//    val v :Vector = Vectors.dense(Array(-.1,-.1))
//    val p :Vector = Vectors.dense(Array(.1,.1))
//    val points = List(Helper.getVN(v))
    val index: (Int, Double) = KMeans.findClosest(points, Helper.getVN(p1))

    dataPts.map(x => (x._1, Vectors.dense(x._2))).map(x => (x._1, Vectors.sqdist(x._2, p1))).collect().sortBy(_._2).foreach(x => println(p(x._1.toString) + "|||" + x._2))


//    println( "###### Point is " + dataPts.toArray()(index._1))
  }



}

