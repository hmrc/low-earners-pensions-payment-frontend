package models.userAnswers

import base.SpecBase
import play.api.libs.json.{JsError, JsObject, JsResult, JsValue, Json}

class SubmissionSummarySpec extends SpecBase {
  "SubmissionSummary" - {
    val model = SubmissionSummary(acceptedIds = Seq("1234"), notAcceptedIds = Seq("5678"))
    val json: JsValue = Json.parse(
      """
        |{
        | "acceptedIds": [
        |   "1234"
        | ],
        | "notAcceptedIds": [
        |   "5678"
        | ]
        |}
      """.stripMargin
    )
    
    "reads" - {
      "should produce the expected JSON" in {
        Json.toJson(model) mustBe json
      }
    }
    
    "writes" - {
      "should produce a JsSuccess for valid JSON" in {
        val result: JsResult[SubmissionSummary] = json.validate[SubmissionSummary] 
        result mustBe a[JsSuccess[_]]
        result.get mustBe model
      }

      "should produce a JsError for invalid JSON" in {
        val result: JsResult[SubmissionSummary] = JsObject.empty.validate[SubmissionSummary]
        result mustBe a[JsError[_]]
      }
    }
  }
}
