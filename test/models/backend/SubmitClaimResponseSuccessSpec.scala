package models.backend

import base.SpecBase
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json}

class SubmitClaimResponseSuccessSpec extends SpecBase {
  "SubmitClaimResponseSuccess" - {
    "reads" - {
      "should return the expected model for valid JSON" in {
        val json = Json.parse(
          """
            |{
            | "updatedLowEarnersOptimisticLock": 1111
            |}
          """.stripMargin
        )
        
        val model: SubmitClaimResponseSuccess = SubmitClaimResponseSuccess(1111)
        
        json.validate[SubmitClaimResponseSuccess] mustBe a[JsSuccess[_]]
        json.as[SubmitClaimResponseSuccess] mustBe model
      }
      
      "should return a JsError for invalid JSON" in {
        JsObject.empty.validate[SubmitClaimResponseSuccess] mustBe a[JsError]
      }
    }
  }
}
