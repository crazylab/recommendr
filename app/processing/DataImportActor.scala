package processing

import java.io.{FileWriter, PrintWriter}
import java.nio.file.Paths

import akka.actor.Actor
import common.Constants._
import common._
import models.TransactionData

class DataImportActor extends Actor{

  def addTransactions(list: Seq[TransactionData]): Unit = {
    Paths.get(Constants.actionLogDataDir).toFile.mkdirs()
    val p: PrintWriter = new PrintWriter(new FileWriter(actionLogFileLocation, true))
    list.map(x => x.productId.foreach(y=>p.println(s"${x.actionType},${x.userId},${x.basketId},${y},${x.location},${x.orderTimestamp}")))
    p.flush()
    p.close()
  }

  override def receive: Receive = {
    case list:Seq[TransactionData] => addTransactions(list)
  }
}
