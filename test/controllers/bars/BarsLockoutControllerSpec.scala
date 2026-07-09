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

package controllers.bars

import base.SpecBase
import controllers.actions.fakes.{FakeDataRetrievalAction, FakeNoRedirectBarsLockoutAction}
import controllers.bars
import models.barsLockout.{BarsVerifyStatusResponse, NumberOfBarsVerifyAttempts}
import models.userAnswers.UserAnswers
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api.Application
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import java.time.Instant
import scala.concurrent.Future

class BarsLockoutControllerSpec extends SpecBase {

  "BarsLockoutController" - {
    trait Test {
      val userAnswers: UserAnswers = UserAnswers(
        id = "id",
        data = Json.obj("leppSummary" -> Json.toJson(summaryModel))
      )
      
      lazy val application: Application = applicationBuilder(userAnswers).build()
    }
    
    "must return OK and the lockout view for exceeding BARS attempts" in new Test {
      override lazy val application: Application = applicationBuilder(
        userAnswers = userAnswers,
        barsFailedAttemptCount = 3,
        barsLockoutTimestampOpt = Some(Instant.ofEpochMilli(1000))
        
      ).build()
      
      running(application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
          method = GET,
          path = bars.routes.BarsLockoutController.onPageLoad().url
        )

        val result: Future[Result] = route(application, request).value
        redirectLocation(result) mustBe None
        status(result) mustEqual OK
      }
    }

    "must redirect to ClearCacheController when user is not locked out" in new Test {
      running(application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
          method = GET,
          path = bars.routes.BarsLockoutController.onPageLoad().url
        )

        val result: Future[Result] = route(application, request).value
        redirectLocation(result) mustBe Some(controllers.routes.ClearCacheController.onPageLoad().url)
      }
    }
  }
}
