package processing

import java.io.File
import java.nio.file.{Files, Paths}

import common.Constants._
import common._
import org.apache.commons.io.FileUtils
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import play.api.Play
import plugin.{JdbcProductScorePersistPlugin, ScorePersistPlugin}

case class TransactionItem(basketId: String, productId: String){

  def asTuple = {
    basketId -> productId
  }
}

object AssociationRuleHelper{
  def uniqCombination(a:Array[String], b:Array[String]) = for {a1<-a; b1<-b if(a1 != b1)} yield (a1,b1)
  def rankItem(itemset: ((String,String), Long), freqMap: collection.Map[String, Int], transactionCount:Long) = {
    val x:Double = freqMap(itemset._1._1)
    val y:Double = freqMap(itemset._1._2)
    val xny:Double = itemset._2
    val n = xny/x
    val dn = 0.00001 + (y-xny)/(1+ transactionCount - x)
    n/dn
  }
}

class AssociationReco() extends Serializable{
  val minSupport : Int = Play.current.configuration.getInt("minimumSupport").get

  val persistPlugin:ScorePersistPlugin = new JdbcProductScorePersistPlugin()

  def addTransactions(sc:SparkContext) = {

    val transactionsFile = new File(actionLogFileLocation)

    var (transactionCount: Long,
         freqMap: RDD[(String, Int)],
         reducedList: RDD[((String, String), Long)]) = loadIntermediateData(sc)

    val transactions = sc.textFile(transactionsFile.toURI.toString).map(_.split(",")).filter(x => x(0) == Constants.offlineBuy).map(x => TransactionItem(x(2), x(3)).asTuple)

    val baskets = transactions.groupByKey().map(x => x._2.toArray)


    val (addedTransactionCount: Long,
          freqMapInc: RDD[(String, Int)],
          additionalPairList: RDD[((String, String), Long)]) = incrementalStepPairCount(baskets)

    transactionCount+=addedTransactionCount
    freqMap = freqMap.union(freqMapInc).reduceByKey(_+_)
    reducedList = reducedList.union(additionalPairList).reduceByKey(_+_)

    persistIntermediateData(sc, transactionCount, freqMap, reducedList)

    val minSupportValueInClosure:Int = minSupport
    val filteredList=reducedList.filter(_._2>minSupportValueInClosure)
    val localFreqMap = freqMap.collectAsMap()
    val scoredList = filteredList.map(x=>(x._1._1, x._1._2, AssociationRuleHelper.rankItem(x,localFreqMap, transactionCount)))
    persistPlugin.store(scoredList)
  }


  def loadIntermediateData(sc: SparkContext): (Long, RDD[(String, Int)], RDD[((String, String), Long)]) = {
    val transactionCount =
      if (checkFileExistence(transactionCountLocation))
        sc.textFile(transactionCountLocation).map(_.toLong).first()
      else
        0L
    val freqMap: RDD[(String, Int)] =
      if (checkFileExistence(freqMapLocation))
        sc.textFile(freqMapLocation).map(_.split(",")).map(x => x(0) -> x(1).toInt)
      else
        sc.emptyRDD[(String, Int)]

    val reducedList: RDD[((String, String), Long)] =
      if (checkFileExistence(reducedListLocation))
        sc.textFile(reducedListLocation).map(x => x.split(",")).map(x => ((x(0), x(1)), x(2).toLong))
      else
        sc.emptyRDD[((String, String), Long)]
    (transactionCount, freqMap, reducedList)
  }


  def persistIntermediateData(sc: SparkContext, transactionCount: Long, freqMap: RDD[(String, Int)], reducedList: RDD[((String, String), Long)]): Unit = {
    Paths.get(Constants.nonPersonalizedDataDir).toFile.mkdirs()
    saveFile(reducedList.map(x => x._1._1 + "," + x._1._2 + "," + x._2), reducedListLocation)
    saveFile(freqMap.map(x => x._1 + "," + x._2), freqMapLocation)

    if (checkFileExistence(transactionCountLocation))
    FileUtils.deleteDirectory(new File(transactionCountLocation))
    sc.makeRDD(List(transactionCount)).saveAsTextFile(transactionCountLocation)
  }

  def saveFile(rdd: RDD[String], dataFileName: String): AnyVal = {
    val fileName: String = dataFileName + "_tmp.txt"  // RDD still on original file, we have to write to temporary file and swap it.(Cause of lazy eval)
    rdd.saveAsTextFile(fileName)
    if (Files.exists(Paths.get(fileName))) {
      FileUtils.deleteDirectory(new File(dataFileName))
      new File(fileName).renameTo(new File(dataFileName))
    }
  }

  def incrementalStepPairCount(rdd: RDD[Array[String]]): (Long, RDD[(String, Int)], RDD[((String, String), Long)]) = {
    val transactionCount: Long = rdd.count()
    val freqMap: RDD[(String, Int)] = rdd.flatMap(x => x.map(_ -> 1)).reduceByKey(_ + _)

    val l = rdd.flatMap(a => AssociationRuleHelper.uniqCombination(a, a)).map(x => (x, 1L))
    val reducedList = l.reduceByKey(_ + _)
    (transactionCount, freqMap, reducedList)
  }

  def checkFileExistence(location: String): Boolean = {
    Files.exists(Paths.get(location))
  }
}

