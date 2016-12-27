package plugin

import models._
import org.apache.spark.rdd.RDD
import play.api.Play
import play.api.db.slick._
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._
//import scala.slick.driver.PostgresDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global

trait ScorePersistPlugin {
  def store(scoreList : RDD[(String,String,Double)])
}

class JdbcProductScorePersistPlugin extends ScorePersistPlugin with HasDatabaseConfig[JdbcProfile]{
//  import driver.api._
  override protected val dbConfig: DatabaseConfig[JdbcProfile] =  DatabaseConfigProvider.get[JdbcProfile](Play.current)
  val productScores:TableQuery[ProductScore] = TableQuery[ProductScore]

  override def store(scoreList: RDD[(String, String, Double)]): Unit = {
    db.run(productScores.delete)
    db.run(
      productScores ++= scoreList.collect()
    )
//    scoreList.foreach(x=>productScores += ()) TODO : should we insert in batch, instead of on bulk insert

  }
}
