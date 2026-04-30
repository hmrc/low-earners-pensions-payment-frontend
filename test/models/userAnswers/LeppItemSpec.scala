package models.userAnswers

import base.SpecBase
import models.userAnswers.LeppItemStatus.Available
import play.api.libs.json.{JsError, JsObject, JsSuccess, JsValue, Json}

class LeppItemSpec extends SpecBase {
  "LeppItem" - {
    val model: LeppItem = LeppItem(
      taxYear = 2024,
      contributions = 1000,
      taxRate = 20,
      entitlement = 200,
      status = Available
    )
    
    "reads" - {
      "should return a JsSuccess for valid JSON" in {
        val json: JsValue = Json.parse(
          """
            |{
            | "taxYear": 2024,
            | "contributions": 1000.00,
            | "taxRate": 20.00,
            | "entitlement": 200.00,
            | "status": "PENDING"
            |}
           """.stripMargin
        )
        
        json.validate[LeppItem] mustBe a[JsSuccess[_]]
        json.as[LeppItem] mustBe model
      }
      
      "should return a JsError for invalid JSON" in {
        JsObject.empty.validate[LeppItem] mustBe a[JsError]
      }
    }
    
    "writes" - {
      "should produce the expected JSON" in {
        val json: JsValue = Json.parse(
          """
            |{
            | "taxYear": 2024,
            | "contributions": 1000.00,
            | "taxRate": 20.00,
            | "entitlement": 200.00,
            | "status": "Available"
            |}
           """.stripMargin
        )
        
        Json.toJson(model) mustBe json
      }
    }
  }

}
