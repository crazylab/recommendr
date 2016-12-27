package org.apache.spark.mllib.clustering
import org.apache.spark.mllib.linalg.{Vectors, Vector}


object Helper {
  def getVN(v : Vector) :VectorWithNorm ={
    new VectorWithNorm(v)
  }
}
