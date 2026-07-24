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
import cats.data.EitherT
import connectors.{BarsVerifyStatusConnector, ConnectorResponse}
import controllers.actions.fakes.{FakeDataRetrievalAction, FakeIdentifierAction}
import controllers.actions.{AcceptPaymentCheckEligibilityAction, RedirectBarsLockoutAction}
import models.ResponseWrapper.{ErrorWrapper, SuccessWrapper}
import models.bars.BarsResponse
import models.barsLockout.{BarsVerifyStatusResponse, NumberOfBarsVerifyAttempts}
import models.errors.ErrorResult.BarsErrorResult
import models.userAnswers.{BankAccountDetails, UserAnswers}
import navigation.Navigator
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import pages.{DashboardPage, SubmissionPage, WhatAreYourBankDetailsPage}
import play.api.Application
import play.api.mvc.Results.ImATeapot
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, route, running, stubMessagesControllerComponents, writeableOf_AnyContentAsEmpty}
import services.{BarsService, LeppSubmissionService, SessionCacheService}
import utils.CorrelationIdHandler
import viewmodels.NormalMode
import views.html.CheckYourAnswersView
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase {
  "CheckYourAnswerController" - {
    
    trait Test {
      val mockBarsService: BarsService = mock[BarsService]
      val mockBarsConnector: BarsVerifyStatusConnector = mock[BarsVerifyStatusConnector]
      
      val controller: CheckYourAnswersController = new CheckYourAnswersController(
        identify = FakeIdentifierAction(nino = nino),
        barsLockout = mock[RedirectBarsLockoutAction],
        getData = FakeDataRetrievalAction(emptyUserAnswers),
        checkEligibility = mock[AcceptPaymentCheckEligibilityAction],
        view = mock[CheckYourAnswersView],
        correlationIdHandler = mock[CorrelationIdHandler],
        barsService = mockBarsService,
        leppSubmissionService = mock[LeppSubmissionService],
        navigator = mock[Navigator],
        sessionService = mock[SessionCacheService],
        barsVerifyStatusConnector = mockBarsConnector,
        controllerComponents = stubMessagesControllerComponents()
      )

      def mockBars(
                    resp: Future[Either[ErrorWrapper, SuccessWrapper[BarsResponse]]]
                  ): OngoingStubbing[ConnectorResponse[BarsResponse]] = when(
        mockBarsService.checkBankAccountDetails(
          barsRequest = ArgumentMatchers.any()
        )(
          hc = ArgumentMatchers.any(),
          ec = ArgumentMatchers.any(),
          cid = ArgumentMatchers.any()
        )
      ).thenReturn(EitherT(resp))

      val bankAccountDetails: BankAccountDetails = BankAccountDetails(
        accountName = "Taxwell Payer",
        accountNumber = "12345678",
        sortCode = "112233",
        rollNumber = Some("1234678")
      )
    }

    "onPageLoad" - {
      "should return OK and the correct view for a GET" in new Test {
        val userAnswers: UserAnswers =
          emptyUserAnswers.set(page = DashboardPage, value = summaryModel).success.value
            .set(page = WhatAreYourBankDetailsPage, value = bankAccountDetails).success.value
            
        val application: Application = applicationBuilder(userAnswers = userAnswers).build()

        running(application) {
          implicit val request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, controllers.routes.CheckYourAnswersController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual OK
        }
      }
      
      "should return to bank details page when no bank details exist" in new Test {
        val userAnswers: UserAnswers =
          emptyUserAnswers.set(page = DashboardPage, value = summaryModel).success.value

        val application: Application = applicationBuilder(userAnswers = userAnswers).build()

        running(application) {
          implicit val request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, controllers.routes.CheckYourAnswersController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.WhatAreYourBankDetailsController.onPageLoad(NormalMode).url)
        }
      }

      "should return to clear cache controller when submission was already completed" in new Test {
        val userAnswers: UserAnswers =
          emptyUserAnswers.set(page = DashboardPage, value = summaryModel).success.value
            .set(page = WhatAreYourBankDetailsPage, value = bankAccountDetails).success.value
            .set(page = SubmissionPage, value = Instant.now()).success.value

        val application: Application = applicationBuilder(userAnswers = userAnswers).build()

        running(application) {
          implicit val request: FakeRequest[AnyContentAsEmpty.type] =
            FakeRequest(GET, controllers.routes.CheckYourAnswersController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(routes.ClearCacheController.onPageLoad().url)
        }
      }
    }
    
    "handleWithBars" - {
      "should redirect correctly for BARS request error result" in new Test {
        when(mockBarsConnector.update())
          .thenReturn(Future(BarsVerifyStatusResponse(NumberOfBarsVerifyAttempts(1), None)))

        mockBars(Future.successful(Left(ErrorWrapper(
          value = BarsErrorResult(status = BAD_REQUEST, code = "BARS_REQUEST_ERRORS"),
          correlationId = testCorrelationId
        ))))

        val result: Future[Result] = controller.handleWithBars(bankAccountDetails)(
          f = () => Future.successful(ImATeapot("Teapot time"))
        )
        
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(bars.routes.BarsRequestErrorsController.onPageLoad().url)
      }
      
      "should redirect correctly for BARS check failed error result" in new Test {
        mockBars(Future.successful(Left(ErrorWrapper(
          value = BarsErrorResult(status = INTERNAL_SERVER_ERROR, code = "BARS_CHECK_FAILED"),
          correlationId = testCorrelationId
        ))))

        val result: Future[Result] = controller.handleWithBars(bankAccountDetails)(
          f = () => Future.successful(ImATeapot("Teapot time"))
        )
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(bars.routes.BarsCheckFailedController.onPageLoad().url)
      }

      "should redirect correctly for any other BARS error result" in new Test {
        mockBars(Future.successful(Left(ErrorWrapper(
          value = BarsErrorResult(status = IM_A_TEAPOT, code = "TEAPOT_TIME"),
          correlationId = testCorrelationId
        ))))
        val result: Future[Result] = controller.handleWithBars(bankAccountDetails)(
          f = () => Future.successful(ImATeapot("Teapot time"))
        )
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(bars.routes.BarsCheckFailedController.onPageLoad().url)
      }

      "should execute block for successful BARS response" in new Test {
        mockBars(Future.successful(Right(SuccessWrapper(
          value = testBarsResponse,
          correlationId = testCorrelationId
        ))))
        val result: Future[Result] = controller.handleWithBars(bankAccountDetails)(
          f = () => Future.successful(ImATeapot("Teapot time"))
        )
        status(result) mustBe IM_A_TEAPOT
        contentAsString(result) mustBe "Teapot time"
      }
    }
  }
}
