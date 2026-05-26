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
import models.userAnswers.LeppItemStatus.Paid
import play.api.libs.json.*

import java.time.LocalDate

class LeppItemSpec extends SpecBase {
  "LeppItem" - {
    val model: LeppItem = LeppItem(
      id = "A-24-1",
      taxYear = 2024,
      contributions = 1000,
      taxRate = 20,
      entitlement = 200,
      status = Paid,
      claimDate = Some(LocalDate.of(2025, 11, 30))
    )

    val json: JsValue = Json.parse(
      """
        |{
        | "id": "A-24-1",
        | "taxYear": 2024,
        | "contributions": 1000.00,
        | "taxRate": 20.00,
        | "entitlement": 200.00,
        | "status": "Paid",
        | "claimDate": "2025-11-30"
        |}
       """.stripMargin
    )
    
    "reads" - {
      "should return a JsSuccess for valid JSON" in {
        json.validate[LeppItem] mustBe a[JsSuccess[_]]
        json.as[LeppItem] mustBe model
      }
      
      "should return a JsError for invalid JSON" in {
        JsObject.empty.validate[LeppItem] mustBe a[JsError]
      }
    }
    
    "writes" - {
      "should produce the expected JSON" in {
        Json.toJson(model) mustBe json
      }
    }

    "formattedEntitlement" - {
      "should return the expected value for a whole pounds number" in {
        model.formattedEntitlement mustBe "£200"
      }

      "should return the expected value for a number with decimals" in {
        model.copy(entitlement = 200.1).formattedEntitlement mustBe "£200.10"
      }
    }

    "formattedContributions" - {
      "should return the expected value for a whole pounds number" in {
        model.formattedContributions mustBe "£1,000"
      }

      "should return the expected value for a number with decimals" in {
        model.copy(contributions = 200.1).formattedContributions mustBe "£200.10"
      }
    }
    
    "apply" - {
      "should correctly construct from NPS model" in {
        LeppItem(2025, calculation, 1) mustBe LeppItem(
          id = "P-2025-1",
          taxYear = 2025,
          contributions = 10.56,
          taxRate = 10.56,
          entitlement = 10.56,
          status = Paid,
          claimDate = Some(LocalDate.of(2023, 6, 27))
        )
      }
    }
  }

}
