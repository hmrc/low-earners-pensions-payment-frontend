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
import play.api.Application
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class BarsCheckFailedControllerISpec extends ControllerIntegrationSpecBase {
  "GET /bank-details-check-failed" when {
    val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
      method = "GET",
      path = "/low-earners-pensions-payment/bank-details-check-failed"
    ).withSession(SessionKeys.authToken -> "auth token")

    testUserAnswersHandling(
      request = request,
      withBankDetailsHandlingTest = true
    )

    "a valid request is made" should {
      "render view correctly" in {
        mockAuthSuccess()
        lazy val application: Application = applicationWithUserAnswers(userAnswersWithBankDetails)

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsString(result) should include("Please try again later")
      }
    }
  }
}