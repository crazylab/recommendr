package models

import slick.driver.PostgresDriver.api._

class Transaction(tag:Tag) extends Table[(String, String)](tag, "new_trx"){
  def user_id : Rep[String] = column[String]("user_id")
  def product_id : Rep[String] = column[String]("product_id")
  def * = (user_id, product_id)
}
