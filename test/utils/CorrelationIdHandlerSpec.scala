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

package utils

import base.SpecBase
import models.CorrelationId
import play.api.test.FakeRequest

class CorrelationIdHandlerSpec extends SpecBase {
  "CorrelationIdHandler" - {
    "getCorrelationId" - {
      val correlationIdHandler: CorrelationIdHandler = new CorrelationIdHandler {
        override protected[utils] def generateCorrelationId: CorrelationId = CorrelationId("generated-id")
      }

      "should generate ID when it does not exist in request headers" in {
        correlationIdHandler.getCorrelationId(FakeRequest()) mustBe CorrelationId("generated-id")
      }

      "should return ID form request headers when it exists" in {
        correlationIdHandler.getCorrelationId(
          FakeRequest().withHeaders("correlationId" -> "some-id")
        ) mustBe CorrelationId("some-id")
      }
    }
  }
}
