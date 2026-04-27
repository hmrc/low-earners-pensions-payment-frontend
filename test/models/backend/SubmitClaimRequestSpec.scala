package models.backend

import base.SpecBase
import models.userAnswers.BankAccountDetails
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.domain.Nino

class SubmitClaimRequestSpec extends SpecBase {
  "SubmitClaimRequest" - {
    "writes" - {
      "should return the expected JSON" in {
        val model: SubmitClaimRequest = SubmitClaimRequest(
          currentLowEarnersOptimisticLock = 11,
          taxYear = 2025,
          accountDetails = BankAccountDetails(
            accountName = "A",
            accountNumber = "11111111",
            sortCode = "112233",
            rollNumber = Some("123456")
          )
        )
        
        val json: JsValue = Json.parse(
          """
            |{
            | "currentLowEarnersOptimisticLock": 11,
            | "taxYear": 2025,
            | "accountDetails": {
            |   "accountName": "A",
            |   "accountNumber": "11111111",
            |   "sortCode": "112233",
            |   "rollNumber": "123456"
            | }
            |}
          """.stripMargin
        )
        
        Json.toJson(model) mustBe json
      }
    }
  }

}
