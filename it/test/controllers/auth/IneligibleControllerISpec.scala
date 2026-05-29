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

package controllers.auth

import base.SpecBase
import controllers.ControllerIntegrationSpecBase
import play.api.Application
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

class IneligibleControllerISpec extends ControllerIntegrationSpecBase {

  "GET /ineligible" when {
    val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
      method = "GET",
      path = "/low-earners-pensions-payment/ineligible"
    ).withSession(SessionKeys.authToken -> "auth token")

    "a valid request is made" should {
      "render view correctly" in {
        mockAuthSuccess()
        lazy val application: Application = fakeApplication()

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        status(result) shouldBe FORBIDDEN
        contentAsString(result) should include("You are not eligible to access this service")
      }
    }
  }
}
