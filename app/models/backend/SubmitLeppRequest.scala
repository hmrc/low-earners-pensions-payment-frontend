package models.backend

import models.userAnswers.BankAccountDetails
import play.api.libs.json.{Json, OWrites}
import uk.gov.hmrc.domain.Nino

case class SubmitLeppRequest(currentLowEarnersOptimisticLock: Int,
                             taxYear: Int,
                             accountDetails: BankAccountDetails)

object SubmitLeppRequest {
  implicit val writes: OWrites[SubmitLeppRequest] = Json.writes[SubmitLeppRequest]
}
