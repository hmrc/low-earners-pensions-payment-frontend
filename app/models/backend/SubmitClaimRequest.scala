package models.backend

import models.userAnswers.BankAccountDetails
import play.api.libs.json.{Json, OWrites}
import uk.gov.hmrc.domain.Nino

case class SubmitClaimRequest(currentLowEarnersOptimisticLock: Int,
                              taxYear: Int,
                              accountDetails: BankAccountDetails)

object SubmitClaimRequest {
  implicit val writes: OWrites[SubmitClaimRequest] = Json.writes[SubmitClaimRequest]
}
