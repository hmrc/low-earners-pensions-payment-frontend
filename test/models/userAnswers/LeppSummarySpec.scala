package models.userAnswers

import base.SpecBase
import models.userAnswers.LeppItemStatus.Available
import play.api.libs.json.{JsError, JsObject, JsSuccess, JsValue, Json}

class LeppSummarySpec extends SpecBase {
  "LeppSummary" - {
    val model: LeppSummary = LeppSummary(
      currentLock = 67,
      items = Seq(
        LeppItem(
          taxYear = 2025,
          contributions = 1000,
          taxRate = 20,
          entitlement = 200,
          status = Available
        )
      )
    )
    
    "reads" - {
      "should return a JsSuccess for valid JSON" in {
        val json: JsValue = Json.parse(
          """
            |{
            | "currentLock": 67,
            | "items": [
            |   {
            |     "taxYear": 2025,
            |     "contributions": 1000.00,
            |     "taxRate": 20.00,
            |     "entitlement": 200.00,
            |     "status": "PENDING"
            |   }
            | ]
            |}
          """.stripMargin
        )
        
        json.validate[LeppSummary] mustBe a[JsSuccess[_]]
        json.as[LeppSummary] mustBe model
      }
      
      "should return a JsError for invalid Json" in {
        JsObject.empty.validate[LeppSummary] mustBe a[JsError]
      }
    }
    
    "writes" - {
      "should return the expected JSON" in {
        val json: JsValue = Json.parse(
          """
            |{
            | "currentLock": 67,
            | "items": [
            |   {
            |     "taxYear": 2025,
            |     "contributions": 1000.00,
            |     "taxRate": 20.00,
            |     "entitlement": 200.00,
            |     "status": "Available"
            |   }
            | ],
            | "submissionCompleted": false
            |}
          """.stripMargin
        )
        
        Json.toJson(model) mustBe json
      }
    }
  }
}
