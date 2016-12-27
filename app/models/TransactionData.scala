package models

import org.joda.time.DateTime

case class TransactionData(actionType : String ,userId: String, basketId: String, productId: Seq[String], location : String, orderTimestamp: DateTime)
