package common

import auth.AccountProvider
import play.api.Play

object Constants {

  val offlineBuy = "offlineBuy"
  val actionLogFileName = "actionLogFile.csv"
  val freqMapFile = "freqMap.txt"
  val transactionCountFile = "transactionCount.txt"
  val reducedListFile = "reducedList.txt"

  def actionLogDataDir = { getAccountBaseDir + "/actions"}

  def actionLogFileLocation = { actionLogDataDir + "/" + actionLogFileName }

  def nonPersonalizedDataDir = { getAccountBaseDir + "/nonpersonalized" }

  def freqMapLocation = { nonPersonalizedDataDir + "/" + freqMapFile }

  def transactionCountLocation = { nonPersonalizedDataDir + "/" + transactionCountFile }

  def reducedListLocation = { nonPersonalizedDataDir + "/" + reducedListFile }

  def personalizedDataDir = { getAccountBaseDir + "/personalized" }

  def alsModelDir = { personalizedDataDir + "/als/model" }

  def getAccountBaseDir():String = { Play.current.configuration.getString("recsysDataDir").getOrElse("./data/") + AccountProvider.account() }
}
