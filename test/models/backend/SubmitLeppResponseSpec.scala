package models.backend

import base.SpecBase
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json}

class SubmitLeppResponseSpec extends SpecBase {
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
        
        val model: SubmitLeppResponse = SubmitLeppResponse(1111)
        
        json.validate[SubmitLeppResponse] mustBe a[JsSuccess[_]]
        json.as[SubmitLeppResponse] mustBe model
      }
      
      "should return a JsError for invalid JSON" in {
        JsObject.empty.validate[SubmitLeppResponse] mustBe a[JsError]
      }
    }
  }
}
