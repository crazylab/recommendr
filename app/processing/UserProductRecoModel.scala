package processing

import java.io.File

import controllers.Global
import org.apache.spark.mllib.recommendation.{Rating, MatrixFactorizationModel}
import org.apache.spark.rdd.RDD
import org.jblas.DoubleMatrix

class UserProductRecoModel(val weightFactor: Array[Double], rank: Int,
                           userFeatures: RDD[(Int, Array[Double])],
                           productFeatures: RDD[(Int, Array[Double])])
  extends MatrixFactorizationModel(rank, userFeatures, productFeatures) {

  override def recommendProducts(user: Int, num: Int): Array[Rating] = {
    recommend(userFeatures.lookup(user).head, productFeatures, num)
      .map(t => Rating(user, t._1, t._2))
  }

  private def recommend(
                         recommendToFeatures: Array[Double],
                         recommendableFeatures: RDD[(Int, Array[Double])],
                         num: Int): Array[(Int, Double)] = {
    val recommendToVector = new DoubleMatrix(recommendToFeatures)
    val scored = recommendableFeatures.map { case (id,features) =>
      (id, recommendToVector.dot(new DoubleMatrix(features).mul(new DoubleMatrix(weightFactor))))
    }
    scored.top(num)(Ordering.by(_._2))
  }

 def withWeightFactor(weightFactor: Array[Double]): UserProductRecoModel = {
    new UserProductRecoModel(weightFactor, this.rank, this.userFeatures, this.productFeatures)
 }

}

object UserProductRecoModel{
  def apply(model:MatrixFactorizationModel): UserProductRecoModel ={

    val weightFactor:Array[Double] = if (new File("model/featureWeightFactors").exists){ Global.ctx.textFile("model/featureWeightFactors").map(_.toDouble).collect() } else new Array[Double](model.rank).map(x=>1.0)
    new UserProductRecoModel(weightFactor, model.rank, model.userFeatures, model.productFeatures)
  }
}
