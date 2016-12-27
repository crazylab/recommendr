package processing


import java.io.File
import java.nio.file.{Files, Paths, Path}

import akka.actor.Actor
import common.Constants
import controllers.Global
import org.apache.commons.io.FileUtils

case object TriggerJob
case object ResetScore

class NonPersonalizedJobProcessingActor extends Actor{

  def updateScores(): Unit = {
    val associationReco = new AssociationReco()
    associationReco.addTransactions(Global.ctx)
  }


  def resetScore(): Unit = {
    val nonPersonalizedDataPath: Path = Paths.get(Constants.nonPersonalizedDataDir)
    if(Files.exists(nonPersonalizedDataPath)) FileUtils.cleanDirectory(nonPersonalizedDataPath.toFile)

    val associationReco = new AssociationReco()
    associationReco.addTransactions(Global.ctx)
  }

  override def receive: Receive = {
    case TriggerJob => updateScores()
    case ResetScore => resetScore()
  }
}
