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
import models.CorrelationId
import models.userAnswers.UserAnswers
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import views.html.SubmitConfirmationView

import scala.concurrent.Future

class SubmitConfirmationControllerISpec extends ControllerIntegrationSpecBase {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val correlationId: CorrelationId = CorrelationId("X-id")
  
  val userAnswersWithZeroSubmission: UserAnswers = UserAnswers(
    id = "1",
    data = Json.obj(
      "leppSummary" -> Json.toJson(summaryModel),
      "bankDetails" -> Json.toJson(bankAccountDetails),
      "leppSubmissionSummary" -> Json.toJson(summaryModel)
    )
  )
  
  "GET /bank-details-received" when {
    val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
      method = "GET",
      path = "/accept-your-low-earners-pension-payment/bank-details-received"
    ).withSession(SessionKeys.authToken -> "auth token")

    "a valid request is made" should {
      "redirect to error page when acceptPayment made no submissions" in {
        mockAuthSuccess()
        
        mockBarsVerifyStatus(status = OK,
          response = Json.obj("attempts" -> 1))
        
        val application: Application = applicationWithUserAnswers(userAnswersWithZeroSubmission)

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )
        
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldEqual Some(routes.SomethingWentWrongController.onPageLoad().url)
      }

      "redirect to clear cache controller when already accepted payments" in {
        mockAuthSuccess()

        mockBarsVerifyStatus(status = OK,
          response = Json.obj("attempts" -> 1))

        val application: Application = applicationWithUserAnswers(userAnswersWithExistingSubmission)

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldEqual Some(routes.ClearCacheController.onPageLoad().url)
      }

      "render view correctly with acceptPayments successful for all payments" in {
        mockAuthSuccess()
        mockBarsVerifyStatus(status = OK,
          response = Json.obj("attempts" -> 1))

        val resultLeppSummaryModel = summaryModel.copy(availableItems = None, acceptedItems = summaryModel.availableItems)

        val userAnswersForSubmission: UserAnswers = UserAnswers(
          id = "1",
          data = Json.obj(
            "leppSummary" -> Json.toJson(summaryModel),
            "bankDetails" -> Json.toJson(bankAccountDetails),
            "leppSubmissionSummary" -> Json.toJson(resultLeppSummaryModel)
          )
        )
        val application: Application = applicationWithUserAnswers(userAnswersForSubmission)

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        val view = application.injector.instanceOf[SubmitConfirmationView]
        val messages: Messages = application.injector.instanceOf[MessagesApi].preferred(request)

        status(result) shouldBe OK
        contentAsString(result) shouldEqual view(resultLeppSummaryModel, "01 January 1970 at 1:00am")(request, messages).toString
      }

      "render view correctly  with acceptPayments partly successful" in {
        mockAuthSuccess()
        mockBarsVerifyStatus(status = OK,
          response = Json.obj("attempts" -> 1))

        val resultLeppSummaryModel = summaryModel.copy(availableItems = None,
          acceptedItems = Some(Seq(summaryModel.availableItems.get.head)))

        val userAnswersForSubmission: UserAnswers = UserAnswers(
          id = "1",
          data = Json.obj(
            "leppSummary" -> Json.toJson(summaryModel),
            "bankDetails" -> Json.toJson(bankAccountDetails),
            "leppSubmissionSummary" -> Json.toJson(resultLeppSummaryModel)
          )
        )
        val application: Application = applicationWithUserAnswers(userAnswersForSubmission)

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        val view = application.injector.instanceOf[SubmitConfirmationView]
        val messages: Messages = application.injector.instanceOf[MessagesApi].preferred(request)

        status(result) shouldBe OK
        contentAsString(result) shouldEqual view(resultLeppSummaryModel, "01 January 1970 at 1:00am")(request, messages).toString
      }
    }
  }
}