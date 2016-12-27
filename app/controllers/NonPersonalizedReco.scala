package controllers

import controllers.Application._
import play.api.libs.json.JsArray
import play.api.mvc.{Action, Controller}
import processing.{ResetScore, TriggerJob}

object NonPersonalizedReco extends Controller {

  def triggerUpdate() = Action {
    Global.jobProcessingActor ! TriggerJob
    Ok("ok")
  }

  def triggerReset() = Action {
    Global.jobProcessingActor ! ResetScore
    Ok("ok")
  }
}
