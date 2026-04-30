package models.userAnswers

import base.SpecBase
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json, JsObject}

class LeppSummarySpec extends SpecBase {
  "LeppSummary" - {
    val model: LeppSummary = LeppSummary(
      currentLock = 67,
      claims = Seq(
        LeppItem(
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
        json.validate[LeppSummary] mustBe a[JsSuccess[_]]
        json.as[LeppSummary] mustBe model
      }
      
      "should return a JsError for invalid Json" in {
        JsObject.empty.validate[LeppSummary] mustBe a[JsError]
      }
    }
    
    "writes" - {
      "should return the expected JSON" in {
        Json.toJson(model) mustBe json
      }
    }
  }
}
