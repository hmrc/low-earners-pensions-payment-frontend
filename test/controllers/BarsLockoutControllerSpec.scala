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

package controllers

import base.SpecBase
import models.barsLockout.{BarsVerifyStatusResponse, NumberOfBarsVerifyAttempts}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import java.time.Instant
import scala.concurrent.Future

class BarsLockoutControllerSpec extends SpecBase {

  "BarsLockoutController" - {
    "must return OK and the lockout view for exceeding BARS attempts" in {
      when(mockBarsConnector.status()(
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any()
      )).thenReturn(Future.successful(BarsVerifyStatusResponse(NumberOfBarsVerifyAttempts(3), Some(Instant.now()))))

      val application = applicationBuilder(emptyUserAnswers).build()

      running(application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, bars.routes.BarsLockoutController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must throw exception when request hit BARS controller without locked out" in {
      val application = applicationBuilder().build()
      when(mockBarsConnector.status()(
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any()
      )).thenReturn(Future.successful(BarsVerifyStatusResponse(NumberOfBarsVerifyAttempts(1), None)))

      running(application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, bars.routes.BarsLockoutController.onPageLoad().url)

        val result = route(application, request).value

        assertThrows[RuntimeException](status(result))
      }
    }
  }
}
