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
import models.userAnswers.LeppItemStatus.Available
import play.api.libs.json.*

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
            |     "status": "Available"
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
