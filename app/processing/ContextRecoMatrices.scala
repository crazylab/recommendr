package processing

import java.io.{BufferedReader, FileReader, PrintWriter}

import org.jblas.DoubleMatrix


object ContextRecoMatrices {

  def load(file: String): Array[DoubleMatrix] = {
    val reader: BufferedReader = new BufferedReader(new FileReader(file))
    val d: Int = Integer.parseInt(reader.readLine())
    val res = new Array[DoubleMatrix](d)
    def readMatrix: DoubleMatrix = {
      val dimension: Array[Int] = reader.readLine().split(",").map(_.toInt)
      new DoubleMatrix(dimension(0), dimension(1), reader.readLine().split(",").map(_.toDouble): _*)
    }
    for (i <- 0 until d) {
      res(i) = readMatrix
    }
    return res
  }

  def save(file: String, m: Array[DoubleMatrix]): Unit = {
    val f = new PrintWriter(file)
    f.println(m.length)
    for (i <- 0 until m.length) {
      f.println(s"${m(i).rows},${m(i).columns},${m(i).length}")
      m(i).data.foreach { x => f.print(x); f.print(",") }
      f.println()
    }
    f.close()
  }
}

