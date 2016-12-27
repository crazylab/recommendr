//import java.io.File
//
//import org.apache.spark.SparkContext
//import org.apache.spark.rdd.RDD
//
//
//object Test {
//
//  def uniqCombination(a:Array[Int], b:Array[Int]) = for {a1<-a; b1<-b if(a1 != b1)} yield (a1,b1)
//
//  def rankItem(itemset: ((Int,Int), Long), freqMap: collection.Map[Int, Int], transactionCount:Long) = {
//    val x:Double = freqMap(itemset._1._1)
//    val y:Double = freqMap(itemset._1._2)
//    val xny:Double = itemset._2
//    val n = xny/x
//    val dn = 0.00001 + (y-xny)/(transactionCount - x)
//    n/dn
//  }
//
//  def test(data:RDD[String],sc:SparkContext, minSupport:Int) = {
//    val transactionCount = 0;
//    val freqMap : RDD[(Int, Int)] = sc.emptyRDD[(Int, Int)]
//    val (addedTransactionCount: Long, freqMapInc: RDD[(Int, Int)], reducedList: RDD[((Int, Int), Long)]) = incrementalStepPairCount(data)
//    val mergedFreqMap = freqMap.union(freqMapInc).reduceByKey(_+_).collectAsMap()
//    val filteredList=reducedList.filter(_._2>minSupport)
//    val scoredList = filteredList.map(x=>x._1 -> (rankItem(x,mergedFreqMap,
//          transactionCount + addedTransactionCount), x._2))
////    val sl = scoredList.sortBy(x=>x._2._1,false,8)
////    sl
//    scoredList
//  }
//
//  def incrementalStepPairCount(data: RDD[String]): (Long, RDD[(Int, Int)], RDD[((Int, Int), Long)]) = {
//    val rdd = data.map { line =>
//      line.split(',').map(_.toInt)
//    }
//    val transactionCount: Long = rdd.count()
//    val freqMap: RDD[(Int, Int)] = rdd.flatMap(x => x.map(_ -> 1)).reduceByKey(_ + _)
//
//    val l = rdd.flatMap(a => uniqCombination(a, a)).map(x => (x, 1L))
//    val reducedList = l.reduceByKey(_ + _)
//    (transactionCount, freqMap, reducedList)
//  }
//
//  def write(score:RDD[((Int,Int), (Double,Long))], productMap:collection.Map[String,String]): Unit ={
//    val fl = new java.io.PrintWriter(new File("/Users/dileepba/data/score.csv"))
//    score.collect().foreach(x => fl.println(x._1._1 + "," + x._1._2 + "," + x._2._1 + "," + x._2._2 + "," + productMap(x._1._1.toString)+ "," + productMap(x._1._2.toString)))
//    fl.close()
//  }
//  def main(args: Array[String]): Unit ={
//    val sc = new SparkContext("local[*]", "BasicAvg", System.getenv("SPARK_HOME"))
//    val rdd = sc.textFile("/Users/dileepba/data/output.dat",8)
//    val productMap:collection.Map[String,String] = sc.textFile("file:/Users/dileepba/data/exp/mahout-t1/product.csv").map{x => x.split(",")(0) -> (x.split(",")(4) + " / " + x.split(",")(5))}.collectAsMap()
//
//    write(test(rdd,sc,5).sortBy(x=>x._2._1,false,8), productMap)
//  }
//}
