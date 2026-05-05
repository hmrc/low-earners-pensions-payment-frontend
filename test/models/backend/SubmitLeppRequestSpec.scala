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
import models.userAnswers.BankAccountDetails
import play.api.libs.json.{JsValue, Json}

class SubmitLeppRequestSpec extends SpecBase {
  "SubmitLeppRequest" - {
    "writes" - {
      "should return the expected JSON" in {
        val model: SubmitLeppRequest = SubmitLeppRequest(
          currentLowEarnersOptimisticLock = 11,
          taxYear = 2025,
          accountDetails = BankAccountDetails(
            accountName = "A",
            accountNumber = "11111111",
            sortCode = "112233",
            rollNumber = Some("123456")
          )
        )
        
        val json: JsValue = Json.parse(
          """
            |{
            | "currentLowEarnersOptimisticLock": 11,
            | "taxYear": 2025,
            | "accountDetails": {
            |   "accountName": "A",
            |   "accountNumber": "11111111",
            |   "sortCode": "112233",
            |   "rollNumber": "123456"
            | }
            |}
          """.stripMargin
        )
        
        Json.toJson(model) mustBe json
      }
    }
  }

}
