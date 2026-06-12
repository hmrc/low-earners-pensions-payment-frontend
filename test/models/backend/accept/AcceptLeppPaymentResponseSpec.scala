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

package models.backend.accept

import base.SpecBase
import play.api.libs.json.*

class AcceptLeppPaymentResponseSpec extends SpecBase {
  "AcceptLeppPaymentResponse" - {
    val model: AcceptLeppPaymentResponse = AcceptLeppPaymentResponse(123)
    val validJson: JsValue = Json.parse("""{"updatedLowEarnersOptimisticLock": 123}""")
    "reads" - {
      "should return the expected model for valid JSON" in {
        validJson.validate[AcceptLeppPaymentResponse] mustBe a[JsSuccess[_]]
        validJson.as[AcceptLeppPaymentResponse] mustBe model
      }
      
      "should return a JsError for invalid JSON" in {
        JsObject.empty.validate[AcceptLeppPaymentResponse] mustBe a[JsError]
      }
    }
  }

}
