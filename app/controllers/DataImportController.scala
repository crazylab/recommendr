package controllers

import models.TransactionData
import org.joda.time.DateTime
import play.api.libs.json.{JsArray, JsObject}
import play.api.mvc.{Action, _}
object DataImportController extends Controller{


  def addActions() = Action(parse.json) {
    request => {
      val transactionArray: JsArray = request.body.as[JsArray]

      val transactions = transactionArray.value.map(x => x.as[JsObject].value).map(x => {
        val productIds = x("productIds").as[JsArray].value.map(x => x.as[String])
        TransactionData(x("actionType").as[String], x("userId").as[String], x("basketId").as[String], productIds, x("location").as[String], DateTime.parse(x("timestamp").as[String]))
      })

      Global.importProcessingActor ! transactions
      Ok("")
    }
  }
}
