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

package controllers.actions

import base.SpecBase
import models.barsLockout.{BarsVerifyStatusResponse, NumberOfBarsVerifyAttempts}
import models.requests.{AuthUser, IdentifierRequest}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.JsObject
import play.api.mvc.*
import play.api.mvc.Results.{Ok, Redirect}
import play.api.test.Helpers.{redirectLocation, status, *}
import play.api.test.{FakeHeaders, FakeRequest}
import utils.CorrelationIdHandler

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class BarsLockoutActionSpec extends SpecBase {

  val headers: FakeHeaders = FakeHeaders(Seq("x-conversation-id" -> "X-id"))

  "BarsLockout Action" - {
    "must redirect to BarsLockoutController when the user lockedout" in {

      when(mockBarsConnector.status()(
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any()
      )).thenReturn(Future.successful(BarsVerifyStatusResponse(NumberOfBarsVerifyAttempts(3), Some(Instant.now()))))

      val application = applicationBuilder(userAnswers = emptyUserAnswers).build()

      running(application) {
        val identifierRequest = IdentifierRequest(FakeRequest(), AuthUser.apply("1", nino, None))

        val barsAction = new BarsLockoutActionRefiner(
          barsVerifyStatusConnector = mockBarsConnector,
          correlationIdHandler = CorrelationIdHandler()
        )

        val result = barsAction.invokeBlock(identifierRequest,
          block = (_: IdentifierRequest[AnyContentAsEmpty.type]) =>
            Future.successful(Redirect(controllers.bars.routes.BarsLockoutController.onPageLoad())))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.bars.routes.BarsLockoutController.onPageLoad().url)
      }
    }

    "must return a Bars request when the user not lockout" in {

      when(mockBarsConnector.status()(
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any()
      )).thenReturn(Future.successful(BarsVerifyStatusResponse(NumberOfBarsVerifyAttempts(1), None)))

      val application = applicationBuilder(userAnswers = emptyUserAnswers).build()

      running(application) {

        val identifierRequest = IdentifierRequest(FakeRequest(), AuthUser("1", nino, None))

        val barsAction = new BarsLockoutActionRefiner(
          barsVerifyStatusConnector = mockBarsConnector,
          correlationIdHandler = CorrelationIdHandler()
        )

        val result = barsAction.invokeBlock(identifierRequest,
          block = (_: IdentifierRequest[AnyContentAsEmpty.type]) => Future.successful(Ok(JsObject.empty)))

        status(result) mustBe OK
      }

    }
  }
}