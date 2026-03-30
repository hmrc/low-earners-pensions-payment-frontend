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

class BarsSubjectSpec extends SpecBase {
  private val testJson: JsValue = Json.parse(
    """
      |{
      | "title": "Mr",
      | "name": "Taxwell Payer",
      | "firstName": "Taxwell",
      | "lastName": "Payer"
      |}
    """.stripMargin)

  "BarsSubject" - {
    "when read from JSON" - {
      "should return a JsSuccess for valid JSON" in {
        val jsResult: JsResult[BarsSubject] = testJson.validate[BarsSubject]
        jsResult mustBe a[JsSuccess[_]]
        jsResult.getOrElse(BarsSubject(None, None, None, None)) mustBe testBarsSubject
      }

      "should return a JsError for invalid JSON" in {
        val jsResult: JsResult[BarsSubject] = Json.parse(
          """
            |{
            | "title": false
            |}
          """.stripMargin).validate[BarsSubject]
        jsResult mustBe a[JsError]
      }
    }

    "when written to JSON" - {
      "should return expected JSON" in {
        val json: JsValue = Json.toJson(testBarsSubject)
        json mustBe testJson
      }
    }
  }
}
