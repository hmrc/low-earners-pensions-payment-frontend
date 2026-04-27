package models.backend

import play.api.libs.json.{Json, Reads}

sealed trait SubmitClaimResponse {
  val updatedLowEarnersOptimisticLock: Int
}

case class SubmitClaimResponseSuccess(updatedLowEarnersOptimisticLock: Int) extends SubmitClaimResponse

object SubmitClaimResponseSuccess {
  implicit val reads: Reads[SubmitClaimResponseSuccess] = Json.reads[SubmitClaimResponseSuccess]
}

case class SubmitClaimResponseWithFailures(updatedLowEarnersOptimisticLock: Int) extends SubmitClaimResponse
