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

class BankAccountDetailsSpec extends SpecBase {
  "BankAccountDetails" - {
    val json: JsValue = Json.parse(
      """
        |{
        | "accountName": "name",
        | "accountNumber": "number",
        | "sortCode": "sortcode",
        | "rollNumber": "rollNumber"
        |}
      """.stripMargin
    )

    val model: BankAccountDetails = BankAccountDetails("name", "number", "sortcode", Some("rollNumber"))

    "reads" - {
      "should return a JsSuccess for valid JSON" in {
        json.validate[BankAccountDetails] mustBe a[JsSuccess[_]]
        json.as[BankAccountDetails] mustBe model
      }

      "should return a JsError when mandatory fields are missing" in {
        Json.parse(
          """
            |{
            | "accountName": true,
            | "accountNumber": 1.1,
            | "sortCode": [],
            | "rollNumber": {}
            |}
          """.stripMargin
        ).validate[BankAccountDetails] mustBe a[JsError]
      }

      "should return a JsError when fields have incorrect data types" in {
        JsObject.empty.validate[BankAccountDetails] mustBe a[JsError]
      }
    }

    "writes" - {
      "should produce the expected JSON" in {
        Json.toJson(model) mustBe json
      }
    }
  }
}
