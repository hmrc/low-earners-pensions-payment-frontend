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
import connectors.ConnectorResponse
import connectors.barsLockout.BarsVerifyStatusConnector
import connectors.barsLockout.model.{BarVerifyStatusId, BarsVerifyStatusResponse, NumberOfBarsVerifyAttempts}
import controllers.actions.{FakeBarsLockoutAction, FakeDataRetrievalAction, FakeIdentifierAction}
import models.ResponseWrapper.{ErrorWrapper, SuccessWrapper}
import models.bars.BarsResponse
import models.errors.ErrorResult.BarsErrorResult
import models.userAnswers.BankAccountDetails
import navigation.Navigator
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api.mvc.Result
import play.api.mvc.Results.ImATeapot
import play.api.test.Helpers.stubMessagesControllerComponents
import services.{BarsService, LeppSubmissionService, SessionCacheService}
import uk.gov.hmrc.domain.Nino
import utils.CorrelationIdOptional
import views.html.{CheckYourAnswersView, ErrorTemplate}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase {
  "CheckYourAnswerController" - {

    trait Test(barsVerifyCount: Int = 1) {
      val mockBarsService: BarsService = mock[BarsService]
      val mockBarsConnector: BarsVerifyStatusConnector = mock[BarsVerifyStatusConnector]
      
      val controller: CheckYourAnswersController = new CheckYourAnswersController(
        identify = FakeIdentifierAction(nino = nino),
        barsLockout = FakeBarsLockoutAction(barsVerifyCount),
        getData = FakeDataRetrievalAction(emptyUserAnswers),
        view = mock[CheckYourAnswersView],
        correlationIdHandler = mock[CorrelationIdOptional],
        barsService = mockBarsService,
        leppSubmissionService = mock[LeppSubmissionService],
        navigator = mock[Navigator],
        errorView = mock[ErrorTemplate],
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
      
      lazy val result: Future[Result] = controller.handleWithBars(bankAccountDetails, Nino(nino))(
        f = () => Future.successful(ImATeapot("Teapot time"))
      )
    }

    "handleWithBars" - {
      "should redirect correctly for BARS request error result" in new Test {
        when(mockBarsConnector.update(BarVerifyStatusId(nino)))
          .thenReturn(Future(BarsVerifyStatusResponse(NumberOfBarsVerifyAttempts(1), None)))
        
        mockBars(Future.successful(Left(ErrorWrapper(
          value = BarsErrorResult(status = BAD_REQUEST, code = "BARS_REQUEST_ERRORS"),
          correlationId = testCorrelationId
        ))))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.bars.routes.BarsRequestErrorsController.onPageLoad().url)
      }
      
      "should redirect correctly for BARS check failed error result" in new Test {
        mockBars(Future.successful(Left(ErrorWrapper(
          value = BarsErrorResult(status = INTERNAL_SERVER_ERROR, code = "BARS_CHECK_FAILED"),
          correlationId = testCorrelationId
        ))))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.bars.routes.BarsCheckFailedController.onPageLoad().url)
      }

      "should redirect correctly for any other BARS error result" in new Test {
        mockBars(Future.successful(Left(ErrorWrapper(
          value = BarsErrorResult(status = IM_A_TEAPOT, code = "TEAPOT_TIME"),
          correlationId = testCorrelationId
        ))))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.bars.routes.BarsCheckFailedController.onPageLoad().url)
      }

      "should execute block for successful BARS response" in new Test {
        mockBars(Future.successful(Right(SuccessWrapper(
          value = testBarsResponse,
          correlationId = testCorrelationId
        ))))

        status(result) mustBe IM_A_TEAPOT
        contentAsString(result) mustBe "Teapot time"
      }
    }
  }
}
