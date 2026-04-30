package models.backend

import play.api.libs.json.{Json, Reads}

case class SubmitLeppResponse(updatedLowEarnersOptimisticLock: Int)

object SubmitLeppResponse {
  implicit val reads: Reads[SubmitLeppResponse] = Json.reads[SubmitLeppResponse]
}
