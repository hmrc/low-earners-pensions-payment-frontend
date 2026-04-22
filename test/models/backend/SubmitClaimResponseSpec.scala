package models.backend

import base.SpecBase
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json}

class SubmitClaimResponseSpec extends SpecBase {
  "SubmitClaimResponse" - {
    "reads" - {
      "should return the expected model for valid JSON" in {
        val json = Json.parse(
          """
            |{
            | "updatedLowEarnersOptimisticLock": 1111
            |}
          """.stripMargin
        )
        
        val model: SubmitClaimResponse = SubmitClaimResponse(1111)
        
        json.validate[SubmitClaimResponse] mustBe a[JsSuccess[_]]
        json.as[SubmitClaimResponse] mustBe model
      }
      
      "should return a JsError for invalid JSON" in {
        JsObject.empty.validate[SubmitClaimResponse] mustBe a[JsError]
      }
    }
  }
}
