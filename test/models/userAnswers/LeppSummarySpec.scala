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
import models.userAnswers.LeppItemStatus.{Available, Cancelled, Paid, Suspended}
import play.api.libs.json.*

class LeppSummarySpec extends SpecBase {
  "LeppSummary" - {
    val model: LeppSummary = LeppSummary(
      currentLock = 67,
      availableItems = Seq(
        LeppItem(
          id = "A-25-1",
          taxYear = 2025,
          contributions = 1000,
          taxRate = 20,
          entitlement = 200,
          status = Available,
          claimDate = None
        )
      ),
      paidItems = Seq(
        LeppItem(
          id = "P-25-1",
          taxYear = 2025,
          contributions = 1000,
          taxRate = 20,
          entitlement = 200,
          status = Paid,
          claimDate = None
        )
      ),
      suspendedItems = Seq(
        LeppItem(
          id = "S-25-1",
          taxYear = 2025,
          contributions = 1000,
          taxRate = 20,
          entitlement = 200,
          status = Suspended,
          claimDate = None
        )
      ),
      cancelledItems = Seq(
        LeppItem(
          id = "C-25-1",
          taxYear = 2025,
          contributions = 1000,
          taxRate = 20,
          entitlement = 200,
          status = Cancelled,
          claimDate = None
        )
      )
    )
    
    val json: JsValue = Json.parse(
      """
        |{
        | "currentLock": 67,
        | "availableItems": [
        |   {
        |     "id": "A-25-1",
        |     "taxYear": 2025,
        |     "contributions": 1000.00,
        |     "taxRate": 20.00,
        |     "entitlement": 200.00,
        |     "status": "Available"
        |   }
        | ],
        | "suspendedItems": [
        |   {
        |     "id": "S-25-1",
        |     "taxYear": 2025,
        |     "contributions": 1000.00,
        |     "taxRate": 20.00,
        |     "entitlement": 200.00,
        |     "status": "Suspended"
        |   }
        | ],
        | "paidItems": [
        |   {
        |     "id": "P-25-1",
        |     "taxYear": 2025,
        |     "contributions": 1000.00,
        |     "taxRate": 20.00,
        |     "entitlement": 200.00,
        |     "status": "Paid"
        |   }
        | ],
        | "cancelledItems": [
        |   {
        |     "id": "C-25-1",
        |     "taxYear": 2025,
        |     "contributions": 1000.00,
        |     "taxRate": 20.00,
        |     "entitlement": 200.00,
        |     "status": "Cancelled"
        |   }
        | ]
        |}
      """.stripMargin
    )
    
    "reads" - {
      "should return a JsSuccess for valid JSON" in {
        json.validate[LeppSummary] mustBe a[JsSuccess[_]]
        json.as[LeppSummary] mustBe model
      }
      
      "should return a JsError for invalid Json" in {
        JsObject.empty.validate[LeppSummary] mustBe a[JsError]
      }
    }
    
    "writes" - {
      "should return the expected JSON" in {
        Json.toJson(model) mustBe json
      }
    }
  }
}
