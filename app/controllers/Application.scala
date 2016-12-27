package controllers


import java.io.{BufferedReader, FileReader}
import java.util
import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import models._
import org.apache.spark.mllib.recommendation.Rating
import org.jblas.DoubleMatrix
import play.api.Play
import play.api.Play.current
import play.api.data.Forms._
import play.api.data._
import play.api.db.slick._
import play.api.libs.concurrent.Akka
import play.api.libs.json.Json._
import play.api.libs.json._
import play.api.mvc._
import processing._
import slick.driver.JdbcProfile

import scala.collection.immutable.IndexedSeq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}


object Application extends Controller with HasDatabaseConfig[JdbcProfile] {
  val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  import driver.api._

  val products = TableQuery[Products]
  val productScores = TableQuery[ProductScore]
  val transactions = TableQuery[Transaction]

  implicit val productFormat = Json.format[Product]
  implicit val writerTuple = new Writes[(String, String)] {
    def writes(t: (String, String)): JsValue = {
      Json.obj(t._1 -> t._2)
    }
  }

  implicit val writer = new Writes[Seq[String]] {
    def writes(t: Seq[String]): JsValue = {
      Json.arr(t.map(x => Json.toJson(x)))
    }
  }

  def index(userId: Option[Int]) = Action async { implicit request =>
    val limit = 50
    val user: String = if (userId.isDefined) userId.getOrElse(0).toString else request.session.get("userId").getOrElse("")

    if (!user.isEmpty) {
      val recommededProducts: Future[Seq[String]] = (Global.recommendationActor ? RecommendProducts(user.toInt, limit)).mapTo[Array[Rating]].map(x => x.map(x=>x.product.toString))
      val recProducts: Seq[String] = Await.result(recommededProducts, Duration(180, "seconds"))
      renderProducts(limit, user, recProducts)
    } else {

      case class ProductIdScore(pid: Rep[String], score: Rep[Option[Double]])
      val q = productScores.groupBy(_.product_id1).map { case (n, c) => (n, c.map(_.score).max) }

      val query = for {
        (p, ps) <- products innerJoin q on (_.product_id === _._1) sortBy (x => x._2._2.desc)
      } yield (p)
      db.run(query.take(limit).result).map(productSeq => Ok(views.html.index(productSeq, userId = "")).withSession("userId" -> user))
    }
  }

  def renderProducts(limit: Int, user: String, recProducts: Seq[String]): Future[Result] = {
    val query: Query[Products, Product, Seq] = productsQuery(recProducts)
    db.run(query.take(limit).result).map(productSeq => Ok(views.html.index(productSeq, user)).withSession("userId" -> user))
  }

  def productsQuery(recProducts: Seq[String]): Application.driver.api.Query[Products, Product, Seq] = {
    val query = for {
      p <- products
      if p.product_id inSetBind recProducts
    } yield p
    query
  }

  def search(term: String) = Action.async {
    val query = for {p <- products if p.brand like s"%$term%"} yield (p.product_id, p.brand)
    db.run(query.take(10).result).map(res => Ok(Json.toJson(res)))
  }

  def productView(pid: Int) = Action.async { implicit request =>
    val limit = 8;

    val productInfo = products.filter(x => x.product_id === pid.toString)
    val p1: Future[Seq[Product]] = db.run(productInfo.result)
    Await.result(p1, Duration(180, "seconds"))

    val q = productScores.filter(_.product_id1 === pid.toString) //d.sortBy(_.score.desc)

    val query = for {
      (p, ps) <- products innerJoin q on (_.product_id === _.product_id2) sortBy (_._2.score.desc)
    } yield (p)

    val recommededProductsBasedOnProfile  : Future[Seq[String]] = (Global.recommendationActor ? RecommendSimilarProducts(pid, limit)).mapTo[Seq[String]]
    val recProductsBasedOnProfile: Seq[String] = Await.result(recommededProductsBasedOnProfile, Duration(180, "seconds"))
    val productsQuery = for (p<-products if p.product_id inSetBind(recProductsBasedOnProfile)) yield p
    val profileBasedProductRec: Seq[Product] = Await.result(db.run(productsQuery.take(limit).result), Duration(180, "second"))

    db.run(query.take(limit).result).map(productSeq => Ok(views.html.product(
      p1.value.get.get(0), productSeq, profileBasedProductRec
    )))
  }

  implicit val timeout = Timeout(1, TimeUnit.MINUTES)

  def userView(uid: Int) = Action async {
    val actorRef = Akka.system.actorSelection("akka://application/user/recommendationActor").resolveOne()
    val actor: ActorRef = Await.result(actorRef, Duration(100000, "millis"))
    actor.ask(RecommendProducts(uid, 10)).mapTo[Rating].map(x => Ok(Json.toJson(x.product)))
  }


  def similarUsers() = Action async { implicit request =>
    val limit = 10
    val userId = request.session.get("userId").get.toInt
    val recommededSimilarUsersBasedOnProfile  : Future[Seq[String]] = (Global.recommendationActor ? RecommendSimilarUsers(userId, limit)).mapTo[Seq[String]]

    recommededSimilarUsersBasedOnProfile.map(userName => Ok(views.html.similarProducts(userName)))
  }

  def logout() = Action {
    Redirect("/").withNewSession
  }

  def orders() = Action async { implicit request =>
    val query = for {
      (trx, p) <- (transactions.filter(_.user_id === request.session.get("userId").getOrElse(""))
        .groupBy(_.product_id).map { case (s, res) => s -> res.length } innerJoin products on (_._1 === _.product_id)).sortBy(_._1._2.desc)
    } yield (p.product_id, p.brand, trx._2)

    db.run(query.take(500).result).map(order => Ok(views.html.orders(order)))
  }

  case class Weight(weight: List[String])

  def factorsSave() = Action { implicit request =>
    val weights:List[Double] = Form("weights" -> list(text) ).bindFromRequest().get.map(_.toDouble)
    Global.recommendationActor ! ChangeWeightFactors(weights.toArray)
    Redirect(routes.Application.factors)
  }

  def factors() = Action { implicit request =>
    val future: Future[Array[(Int, Array[Double])]] = (Global.recommendationActor ? GetProductFeatures).mapTo[Array[(Int, Array[Double])]]
    val features: Array[(Int, Array[Double])] = Await.result(future, Duration(180, "seconds"))
    val futureForWeight: Future[Array[Double]] = (Global.recommendationActor ? GetWeightFactors).mapTo[Array[Double]]
    val weightFactors: Array[Double] = Await.result(futureForWeight, Duration(180, "seconds"))
    val seq: IndexedSeq[Future[Seq[Product]]] = for (index <- Range(0, features(0)._2.length)) yield {

      val sortedFactor: Array[(Int, Array[Double])] = features.sortBy(x => -x._2(index))

      val pids = sortedFactor.take(10).map(_._1.toString)

      val query: Query[Products, Product, Seq] = productsQuery(pids)
      db.run(query.result)
    }


    Ok(views.html.factors(seq.map(x => Await.result(x, Duration(180, "seconds"))), weightFactors, "1"))
  }

  def context() = Action { implicit request =>
    val userId: String = request.session.get("userId").getOrElse("")
    val id = userId.toInt - 1999
    if (id >0 && id<100) Ok("")
    val prodMap = new util.HashMap[Int, String]()
    val reader: BufferedReader = new BufferedReader(new FileReader("data_min_prod.csv"))
    var line = reader.readLine()
    var c = 1
    while (line != null) {
      prodMap.put(c, line.trim)
      line = reader.readLine()
      c+=1
    }

    val m: Array[DoubleMatrix] = ContextRecoMatrices.load("matv2.small")

    ContextRecoMatrices.save("matv2_new.small", m)
      val products: IndexedSeq[IndexedSeq[(String, String, Int)]] = for (l<- 1 to m(2).columns) yield (1 to m(1).columns map (x => x -> rate(m, id, x, l))).sortBy(_._2).reverse.take(20).map(x=>(prodMap.get(x._1),x._2.toString,1))
    Ok(views.html.context(products))
  }

  def rate(m: Array[DoubleMatrix], i: Int, j: Int, l: Int): Double = {
    return m(0).getColumn(i-1).mul(m(1).getColumn(j-1)).mul(m(2).getColumn(l-1)).columnSums().get(0)
  }
}