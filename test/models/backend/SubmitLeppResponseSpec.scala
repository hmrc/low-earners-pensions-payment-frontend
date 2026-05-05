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
