package models.backend

import play.api.libs.json.{Json, Reads}

case class SubmitClaimResponse(updatedLowEarnersOptimisticLock: Int)

object SubmitClaimResponse {
  implicit val reads: Reads[SubmitClaimResponse] = Json.reads[SubmitClaimResponse]
}
