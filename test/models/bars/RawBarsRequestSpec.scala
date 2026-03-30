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

class RawBarsRequestSpec extends SpecBase {
  "RawBarsRequest" - {
    "should return a JsSuccess when read from valid JSON" in {
      val json: JsValue = Json.parse(
        """
          |{
          | "name": "Taxwell Payer",
          | "sortCode": "11-22-33",
          | "accountNumber": "12345678",
          | "rollNumber": "rollNumber"
          |}
        """.stripMargin 
      )
      
      val result: JsResult[RawBarsRequest] = json.validate[RawBarsRequest] 
      result mustBe a[JsSuccess[_]]
      result.getOrElse(RawBarsRequest(None, None, None, None))
    }

    "should return a JsError when read from invalid JSON" in {
      val json: JsValue = Json.parse(
        """
          |{
          | "name": false
          |}
        """.stripMargin)
      val result: JsResult[RawBarsRequest] = json.validate[RawBarsRequest]
      result mustBe a[JsError]
    }
  }
}
