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

import cats.data.EitherT
import models.CorrelationId
import models.ResponseWrapper.{ErrorWrapper, SuccessWrapper}
import models.backend.{SubmitLeppRequest, SubmitLeppResponse}
import models.errors.ErrorResult.ServiceErrorResult
import models.userAnswers.LeppItemStatus.{Available, Paid}
import models.userAnswers.{LeppItem, LeppSummary, UserAnswers}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when as mockitoWhen
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{route, writeableOf_AnyContentAsEmpty}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.http.SessionKeys
import viewmodels.NormalMode
import viewmodels.checkYourAnswers.CheckYourAnswersSummary.cyaSummaryList
import viewmodels.formPages.FormPageViewModel
import views.html.{CheckYourAnswersView, ErrorTemplate}

import scala.concurrent.Future

class CheckYourAnswersControllerISpec extends ControllerIntegrationSpecBase {
  
  val barsVerifyStatusUrl = ""
  "GET /check-your-answers" when {
    val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
      method = "GET",
      path = "/low-earners-pensions-payment/check-your-answers"
    ).withSession(SessionKeys.authToken -> "auth token")

    val barsResponse = s"""{"attempts":3,"lockoutExpiryDateTime":"${expectedLockout.toString}"}"""
    
    "a valid request is made" should {
      "return the expected view" in {
        mockAuthSuccess()
        mockBarsAction(url = "/low-earners-pensions-payment/bars/verify/status",
          response = Json.parse(s"""{"attempts":1}""".stripMargin))
        
        val application: Application = applicationWithUserAnswers(userAnswers)

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        val view = application.injector.instanceOf[CheckYourAnswersView]
        implicit val messages: Messages = application.injector.instanceOf[MessagesApi].preferred(request)

        val rows: Seq[SummaryListRow] = cyaSummaryList(bankAccountDetails)
        val viewModel: FormPageViewModel = FormPageViewModel(
          onSubmit = routes.CheckYourAnswersController.onSubmit(),
          backLinkUrl = Some(routes.WhatAreYourBankDetailsController.onPageLoad(NormalMode).url)
        )

        status(result) shouldBe OK
        contentAsString(result) shouldEqual view(rows, viewModel)(request, messages).toString
      }
    }
  }

  "POST /check-your-answers" when {
    def request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
      method = "POST",
      path = "/low-earners-pensions-payment/check-your-answers"
    )
      .withSession(SessionKeys.authToken -> "auth token")

    "backend returns an error for any LEPP submissions" should {
      "redirect to error page" in {
        mockAuthSuccess()
        
        val application: Application = applicationWithUserAnswers(userAnswers)

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        mockitoWhen(
          fakeConnector.submitLepp(
            request = ArgumentMatchers.any()
          )(
            hc = ArgumentMatchers.any(),
            ec = ArgumentMatchers.any(),
            cid = ArgumentMatchers.any()
          )
        ).thenReturn(EitherT(Future.successful(
          Left(ErrorWrapper(
            value = ServiceErrorResult(IM_A_TEAPOT, "Teapot time!"),
            correlationId = CorrelationId("cid")))
        )))

        val view = application.injector.instanceOf[ErrorTemplate]
        implicit val messages: Messages = application.injector.instanceOf[MessagesApi].preferred(request)
        
        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsString(result) shouldBe view("title", "heading", "message")(request, messages).toString
      }
    }
    
    "submission succeeds for a single available LEPP item" should {
      "redirect to confirmation page" in {
        mockAuthSuccess()
        val application: Application = applicationWithUserAnswers(userAnswers)

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        mockitoWhen(
          fakeConnector.submitLepp(
            request = ArgumentMatchers.any()
          )(
            hc = ArgumentMatchers.any(),
            ec = ArgumentMatchers.any(),
            cid = ArgumentMatchers.any()
          )
        ).thenReturn(EitherT(Future.successful(
          Right(SuccessWrapper(
            value = SubmitLeppResponse(updatedLowEarnersOptimisticLock = 2),
            correlationId = CorrelationId("cid")))
        )))
        
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.SubmitConfirmationController.onPageLoad().url)
      }
    }

    "submission succeeds for a multiple available LEPP items" should {
      "redirect to confirmation page" in {
        mockAuthSuccess()

        val summaryModel: LeppSummary = LeppSummary(
          currentLock = 67,
          items = Seq(
            LeppItem(
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Available
            ),
            LeppItem(
              taxYear = 2026,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Available
            ),
            LeppItem(
              taxYear = 2024,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Paid
            )
          )
        )

        val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = Json.obj(
            "leppSummary" -> Json.toJson(summaryModel),
            "bankDetails" -> Json.toJson(bankAccountDetails)
          )
        )
        
        val application: Application = applicationWithUserAnswers(userAnswers)

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        mockitoWhen(
          fakeConnector.submitLepp(
            request = ArgumentMatchers.eq(
              SubmitLeppRequest(
                currentLowEarnersOptimisticLock = 67,
                taxYear = 2025,
                accountDetails = bankAccountDetails
              )
            )
          )(
            hc = ArgumentMatchers.any(),
            ec = ArgumentMatchers.any(),
            cid = ArgumentMatchers.any()
          )
        ).thenReturn(EitherT(Future.successful(
          Right(SuccessWrapper(
            value = SubmitLeppResponse(updatedLowEarnersOptimisticLock = 68),
            correlationId = CorrelationId("cid")))
        )))

        mockitoWhen(
          fakeConnector.submitLepp(
            request = ArgumentMatchers.eq(
              SubmitLeppRequest(
                currentLowEarnersOptimisticLock = 68,
                taxYear = 2026,
                accountDetails = bankAccountDetails
              )
            )
          )(
            hc = ArgumentMatchers.any(),
            ec = ArgumentMatchers.any(),
            cid = ArgumentMatchers.any()
          )
        ).thenReturn(EitherT(Future.successful(
          Right(SuccessWrapper(
            value = SubmitLeppResponse(updatedLowEarnersOptimisticLock = 69),
            correlationId = CorrelationId("cid")))
        )))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.SubmitConfirmationController.onPageLoad().url)
      }
    }
  }

}
