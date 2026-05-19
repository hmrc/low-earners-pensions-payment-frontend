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

import common.IntegrationSpecBase
import controllers.ControllerIntegrationSpecBase
import models.userAnswers.*
import models.userAnswers.LeppItemStatus.Available
import play.api.Application
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

  class BarsRequestErrorsControllerISpec extends ControllerIntegrationSpecBase {
    "GET /bank-details-check-errors" when {
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
        method = "GET",
        path = "/low-earners-pensions-payment/bank-details-check-errors"
      ).withSession(SessionKeys.authToken -> "auth token")
      
      testControllerAuth(request)
      testSessionDataHandling(request)
      testLeppDataHandling(request)
      
      "should return the expected view for a successful request" in {
        mockAuthSuccess()

        val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = Json.obj(
            "leppSummary" -> Json.toJson(summaryModel)
          )
        )

        lazy val application: Application = applicationWithUserAnswers(userAnswers)

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("If this issue persists you may have to contact HMRC")
      }
    }
}
