package models.userAnswers

import base.SpecBase
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json, JsObject}

class ClaimSummarySpec extends SpecBase {
  "ClaimSummary" - {
    val model: ClaimsSummary = ClaimsSummary(
      currentLock = 67,
      claims = Seq(
        ClaimItem(
          taxYear = 2025,
          contributions = 1000,
          taxRate = 20,
          entitlement = 200,
          claimed = false
        )
      )
    )
    
    val json: JsValue = Json.parse(
      """
        |{
        | "currentLock": 67,
        | "claims": [
        |   {
        |     "taxYear": 2025,
        |     "contributions": 1000.00,
        |     "taxRate": 20.00,
        |     "entitlement": 200.00,
        |     "claimed": false
        |   }
        | ]
        |}
      """.stripMargin
    )
    "reads" - {
      "should return a JsSuccess for valid JSON" in {
        json.validate[ClaimsSummary] mustBe a[JsSuccess[_]]
        json.as[ClaimsSummary] mustBe model
      }
      
      "should return a JsError for invalid Json" in {
        JsObject.empty.validate[ClaimsSummary] mustBe a[JsError]
      }
    }
    
    "writes" - {
      "should return the expected JSON" in {
        Json.toJson(model) mustBe json
      }
    }
  }
}
