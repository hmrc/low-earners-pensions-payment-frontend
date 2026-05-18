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

import common.IntegrationSpecBase
import models.userAnswers.*
import models.userAnswers.LeppItemStatus.Available
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.{JsBoolean, Json}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.SessionKeys
import views.html.SubmitConfirmationView

import scala.concurrent.Future

class SubmitConfirmationControllerISpec extends ControllerIntegrationSpecBase {
  
  "GET /confirmation" when {
    val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
      method = "GET",
      path = "/low-earners-pensions-payment/confirmation"
    ).withSession(SessionKeys.authToken -> "auth token")

    testAuthForRequest(request)

    testUserAnswersHandling(
      request = request,
      withBankDetailsHandlingTest = true,
      submissionShouldExist = true
    )

    "a valid request is made" should {
      "render view correctly" in {
        mockAuthSuccess()

        val application: Application = applicationWithUserAnswers(userAnswersWithExistingSubmission)

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        val view = application.injector.instanceOf[SubmitConfirmationView]
        val messages: Messages = application.injector.instanceOf[MessagesApi].preferred(request)

        status(result) shouldBe OK
        contentAsString(result) shouldEqual view("01 January 1970 at 1:00am")(request, messages).toString
      }
    } 
  }
}