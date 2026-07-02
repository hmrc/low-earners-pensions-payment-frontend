/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.userAnswers

import base.SpecBase
import play.api.libs.json.*

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
        result mustBe a[JsError]
      }
    }
  }
}
