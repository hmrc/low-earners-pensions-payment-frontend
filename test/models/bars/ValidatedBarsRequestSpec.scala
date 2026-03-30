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

package models.bars

import base.SpecBase
import play.api.libs.json.{JsValue, Json}

class ValidatedBarsRequestSpec extends SpecBase {

  private val testJson: JsValue = Json.parse(
    """
      |{
      | "subject": {
      |   "name": "Taxwell Payer"
      | },
      | "account": {
      |   "sortCode": "112233",
      |   "accountNumber": "12345678",
      |   "rollNumber": "rollNumber"
      | }
      |}
    """.stripMargin)

  "BarsRequest" - {
    "when written to JSON" - {
      "should return expected JSON" in {
        val json: JsValue = Json.toJson(testValidatedBarsRequest)
        json mustBe testJson
      }
    }
  }
}
