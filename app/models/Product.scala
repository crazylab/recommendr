package models

import org.apache.spark.sql.catalyst.expressions.NonNullLiteral
import slick.driver.PostgresDriver.api._

//case class Product(product_id: Pk[String], brand: String)
case class Product(product_id :String, description:String, sub_desc:String, manufacturer: String, dept:String, brand:String, size:String)

class Products(tag: Tag) extends Table[Product](tag, "products"){
  def product_id:Rep[String] = column[String]("product_id", O.PrimaryKey)
  def description:Rep[String]  = column[String]("description")
  def sub_desc:Rep[String]     = column[String]("sub_desc")
  def manufacturer:Rep[String] = column[String]("manufacturer")
  def dept:Rep[String]         = column[String]("dept")
  def brand:Rep[String]        = column[String]("brand")
  def size:Rep[String]         = column[String]("size")

  //  override def * : ProvenShape[(String, String, String, String, String, String, String)] =
  //    (product_id, description, sub_desc, manufacturer, dept, brand, size)
  def * = (product_id, description, sub_desc, manufacturer, dept, brand, size) <>(Product.tupled, Product.unapply _)


}

class ProductScore(tag:Tag) extends Table[(String, String, Double)](tag, "product_score"){
  def product_id1 : Rep[String] = column[String]("product_id1")
  def product_id2 : Rep[String] = column[String]("product_id2")
  def score : Rep[Double] = column[Double]("score")
  def * = (product_id1, product_id2, score)
}
//
//object Product {
////  implicit val pkWrites = play.api.libs.json.Json.writes[Pk[String]]
//  val simple = {
//    get[Pk[String]]("product_id") ~
//      get[String]("brand") map {
//      case product_id~brand => Product(product_id, brand)
//    }
//  }
//  def find(term:String): List[Product] ={
//    val products = DB.withConnection {
//      implicit connection => SQL( "select * from products where brand like {term}")
//        .on("term" -> ("%" + term + "%"))
//        .as(Product.simple *)
//    }
//    println(products)
//    products
//  }
//}
