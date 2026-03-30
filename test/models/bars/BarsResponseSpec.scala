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
import play.api.libs.json.*

class BarsResponseSpec extends SpecBase {
  private val testJson: JsValue = Json.parse(
    """
      |{
      | "nonStandardAccountDetailsRequiredForBacs": "no",
      | "sortCodeSupportsDirectDebit": "yes",
      | "sortCodeSupportsDirectCredit": "yes",
      | "accountNumberIsWellFormatted": "indeterminate",
      | "nameMatches": "indeterminate",
      | "sortCodeIsPresentOnEISCD": "yes",
      | "sortCodeBankName": "Test",
      | "accountExists": "no",
      | "accountName": "Taxwell Payer",
      | "iban": "test-iban"
      |}
    """.stripMargin)

  private val testModel: BarsResponse = BarsResponse(
    accountNumberIsWellFormatted = "indeterminate",
    accountExists = "no",
    nameMatches = "indeterminate",
    accountName = Some("Taxwell Payer"),
    nonStandardAccountDetailsRequiredForBacs = "no",
    sortCodeIsPresentOnEISCD = "yes",
    sortCodeSupportsDirectDebit = "yes",
    sortCodeSupportsDirectCredit = "yes",
    sortCodeBankName = Some("Test"),
    iban = Some("test-iban")
  )
  
  "BarsResponse" - {
    "when read from JSON" - {
      "should return a JsSuccess for valid JSON" in {
        val fillerModel = BarsResponse("N/A", "N/A", "N/A", None, "N/A", "N/A", "N/A", "N/A", None, None)
        val jsResult: JsResult[BarsResponse] = testJson.validate[BarsResponse]
        jsResult mustBe a[JsSuccess[_]]
        jsResult.getOrElse(fillerModel) mustBe testModel
      }

      "should return a JsError for invalid JSON" in {
        val jsResult: JsResult[BarsResponse] = Json.parse(
          """
            |{
            | "title": false
            |}
          """.stripMargin).validate[BarsResponse]
        jsResult mustBe a[JsError]
      }
    }

    "when written to JSON" - {
      "should return expected JSON" in {
        val json: JsValue = Json.toJson(testModel)
        json mustBe testJson
      }
    }
  }
}
