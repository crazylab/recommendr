package test

import java.io.PrintWriter
import java.util

import com.github.fommil.netlib.{LAPACK}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.jblas.DoubleMatrix
import org.netlib.util.intW

import scala.collection.mutable

object Itals {


  def main(arg: Array[String]): Unit = {

//    val data = List((List(1, 1, 1), 1),
//      (List(2, 1, 1), 1),
//      (List(4, 1, 1), 1),
//      (List(5, 1, 1), 1),
//      (List(8, 1, 1), 1),
//      (List(2, 2, 1), 1),
//      (List(8, 2, 2), 1),
//      (List(2, 3, 2), 1),
//      (List(4, 3, 1), 1),
//      (List(5, 3, 1), 1),
//      (List(8, 3, 2), 1),
//      (List(3, 4, 1), 1),
//      (List(6, 4, 1), 1),
//      (List(7, 4, 1), 1),
//      (List(3, 5, 2), 1),
//      (List(6, 5, 1), 1),
//      (List(7, 5, 1), 1),
//      (List(3, 6, 2), 1),
//      (List(5, 6, 2), 1))

    val data = List((List(1, 1, 1), 1),
      (List(2, 1, 1), 1),
      (List(4, 1, 1), 1),
      (List(5, 1, 1), 1),
      (List(8, 1, 1), 1),
      (List(2, 2, 1), 1),
      (List(8, 2, 1), 1),
      (List(2, 3, 1), 1),
      (List(4, 3, 1), 1),
      (List(5, 3, 1), 1),
      (List(8, 3, 1), 1),
      (List(3, 4, 1), 1),
      (List(6, 4, 1), 1),
      (List(7, 4, 1), 1),
      (List(3, 5, 1), 1),
      (List(6, 5, 1), 1),
      (List(7, 5, 1), 1),
      (List(3, 6, 1), 1),
      (List(5, 6, 1), 1))
    val sc = new SparkContext("local[*]", "name")


    val product: RDD[Int] = sc.textFile("data_min_prod.csv").map(_.toInt)
    var productMap = new util.HashMap[Int, Int]()
    var pi:Int = 0
    for (x <- product.collect()) {
      pi+=1
      productMap.put(x, pi)
    }
    val rating: RDD[(List[Int], Int)] = sc.textFile("data_min.csv",8).map(x=>x.split(",").map(_.toInt)).map(x=> List[Int](x(0).toInt, productMap.get(x(1).toInt), x(2).toInt) -> 1)
    rating.persist()

    val d = 3
    var m = new Array[DoubleMatrix](d)
    var mmt = new Array[DoubleMatrix](d)
    val k = 9
    val s = List(100,2100,4)
    for (i <- 0 until d) {
//      if (i == d-1)
        m(i) = DoubleMatrix.randn(k, s(i))
//      else
//        m(i) = DoubleMatrix.ones(k, s(i))
      mmt(i) = m(i).mmul(m(i).transpose())
    }
    for (e <- 0 until 20) {
      trainIteration(rating, d, m, mmt, k, s)
//      rateAll(d, m)
    }

    save("matv2.small", m)
    //rateAll(d, m)

  }

  def trainIteration(rating: RDD[(List[Int], Int)], d: Int, m: Array[DoubleMatrix], mmt: Array[DoubleMatrix], k: Int, s: List[Int]): Unit = {
    for (i <- 0 until d) {

      val C = (for (mi <- 0 until d if mi != i) yield mmt(mi)).reduce((x, y) => x.mul(y))

      for (j <- 0 until s(i)) {
        val dd: RDD[(List[Int], Int)] = rating.filter(x => (x._1(i) - 1) == j)
        val vs: RDD[DoubleMatrix] = dd.map(x => (for (ii <- 0 until d if ii != i) yield m(ii).getColumn(x._1(ii) - 1)).reduce((x, y) => x.mul(y)))
        vs.persist()
        if (vs.count() > 0) {
          val b: DoubleMatrix = vs.reduce((x, y) => x.add(y))
          val f2: DoubleMatrix = vs.map(v => v.mmul(v.transpose())).reduce((x, y) => x.add(y))
          val A = f2.add(C)
          val arr = new Array[Int](k)
          val info: intW = new intW(0)
          val bCopy: DoubleMatrix = new DoubleMatrix()
          bCopy.copy(b)
          val s = new Array[Double](k * 8)
          val lpack: LAPACK = LAPACK.getInstance()
          lpack.dgetrf(k, k, A.data, k, arr, info)
          lpack.dgetri(k, A.data, k, arr, s, k * 8, info)

          val col = A.mmul(b)
          m(i).putColumn(j, col)
        }
      }
      mmt(i) = m(i).mmul(m(i).transpose())
      println(m(0).getColumn(6).mul(m(1).getColumn(1)).mul(m(2).getColumn(1)))
    }
  }

  def rateAll(d: Int, m: Array[DoubleMatrix]): Unit = {
    println("weights ..")

    for (i <- 0 until d) {
      println ("m " + (i+1))
      for(r <- 0 until 10) {
        for (c <- 0 until 10)
          printf("\t%6.2f", m(i).get(r, c))
        println()
      }
    }
    println("Location 1")
    printRatings(m, 0)

    println("Location 2")
    printRatings(m, 1)
  }

  def printRatings(m: Array[DoubleMatrix], l:Int): Unit = {
    for (i <- 0 until 8) {
      for (j <- 0 until 6)
        rate(m, i, j, l)
      println()
    }
  }

  def rate(m: Array[DoubleMatrix], i: Int, j: Int, l: Int): Unit = {
    printf("\t%10f",m(0).getColumn(i-1).mul(m(1).getColumn(j-1)).mul(m(2).getColumn(l-1)).columnSums().get(0))
  }
  def save(m: Array[DoubleMatrix]):Unit = {
    val f = new PrintWriter("mat.data")
    f.println(m.length)
    for (i<-0 until m.length){
      f.println(i)
      m(i).data.foreach {x => f.print(x); f.print(",")}
      f.println()
    }
    f.close()
  }
  def save(file:String, m:Array[DoubleMatrix]):Unit = {
    val f = new PrintWriter(file)
    f.println(m.length)
    for (i<-0 until m.length){
      f.println(s"${m(i).rows},${m(i).columns},${m(i).length}")
      m(i).data.foreach {x => f.print(x); f.print(",")}
      f.println()
    }
    f.close()
  }
}
