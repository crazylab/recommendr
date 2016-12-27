package models

case class OfflineBuyEvent(userId : String, productId: String){

  def getUserId: Int = {
    userId.trim.toInt
  }

  def getProductId: Int = {
    productId.trim.toInt
  }

  def asTuple = {
    (getUserId, getProductId)
  }
}
