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

package controllers.common

import base.SpecBase
import controllers.actions.*
import controllers.actions.fakes.{FakeDataRetrievalAction, FakeIdentifierAction, FakeRedirectBarsLockoutAction}
import controllers.bars
import models.userAnswers.{BankAccountDetails, SubmissionSummary, UserAnswers}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Results.ImATeapot
import play.api.mvc.{DefaultActionBuilder, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesControllerComponents
import viewmodels.NormalMode

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BarsLeppBaseControllerSpec extends SpecBase {
  private trait Test(count: Int = 0) {
    val userAnswers: UserAnswers = UserAnswers("1")
    val mockAuth: IdentifierAction = FakeIdentifierAction(nino = nino)
    private val mockBarsLockoutAction: FakeRedirectBarsLockoutAction = FakeRedirectBarsLockoutAction(count)
    private val mockCc: MessagesControllerComponents = stubMessagesControllerComponents()
    private lazy val mockData: DataRetrievalAction = FakeDataRetrievalAction(userAnswers)
    private lazy val mockEligibilityAction: CheckEligibilityAction = new AcceptPaymentCheckEligibilityAction()

    val defaultActionBuilder: DefaultActionBuilder = app.injector.instanceOf[DefaultActionBuilder]
    
    class DummyController(identifierAction: IdentifierAction,
                          barsLockoutAction: BarsLockoutAction,
                          data: DataRetrievalAction,
                          checkEligibility: CheckEligibilityAction,
                          val controllerComponents: MessagesControllerComponents = mockCc)
      extends BarsLeppBaseController(identifierAction, barsLockoutAction, data, checkEligibility)
    
    lazy val controller: DummyController = new DummyController(
      mockAuth,
      mockBarsLockoutAction,
      mockData,
      mockEligibilityAction
    )
  }
  
  "BarsLeppBaseController" - {
    "handle" - {
      "should invoke bars refiner when authorisation succeeds" in new Test(1) {
        override val userAnswers: UserAnswers = emptyUserAnswers.copy(
          data = Json.obj(
            "leppSummary" -> Json.toJson(summaryModel)
          )
        )
          
        val result: Future[Result] = controller.handle(
          _ => Future.successful(ImATeapot("Teapot time"))
        )(FakeRequest())
        
        status(result) mustBe IM_A_TEAPOT
        contentAsString(result) mustBe "Teapot time"
      }

      "should not bars refiner when authorisation fails" in new Test(1) {
        override val mockAuth = FakeIdentifierAction(true, nino)
        
        val result: Future[Result] = controller.handle(
          _ => Future.successful(ImATeapot("Teapot time"))
        )(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("some-url")
      }

      "should redirect to BarsLockout when authorisation succeed but exceeds BARS limit" in new Test(3) {
        val result: Future[Result] = controller.handle(
          _ => Future.successful(ImATeapot("Teapot time"))
        )(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(bars.routes.BarsLockoutController.onPageLoad().url)
      }
    }

    "handleWithBankDetails" - {
      "should redirect to dashboard page when claims data isn't cached" in new Test {
        val result: Future[Result] = controller.handleWithBankDetails(
          _ => _ => Future.successful(ImATeapot(""))
        )(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.DashboardController.onPageLoad().url)
      }

      "should redirect to bank details page when bank details aren't cached" in new Test {
        override val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = Json.obj("leppSummary" -> Json.toJson(summaryModel))
        )

        val result: Future[Result] = controller.handleWithBankDetails(
          _ => _ => Future.successful(ImATeapot(""))
        )(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.WhatAreYourBankDetailsController.onPageLoad(NormalMode).url)
      }

      "should evaluate block when all data is cached" in new Test {
        val detailsModel: BankAccountDetails = BankAccountDetails(
          accountName = "name nameson",
          accountNumber = "112233",
          sortCode = "12345678",
          rollNumber = None
        )

        override val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = Json.obj(
            "leppSummary" -> Json.toJson(summaryModel),
            "bankDetails" -> Json.toJson(detailsModel)
          )
        )

        val result: Future[Result] = controller.handleWithBankDetails(
          _ => _ => Future.successful(ImATeapot("teapot time"))
        )(FakeRequest())

        status(result) mustBe IM_A_TEAPOT
        contentAsString(result) mustBe "teapot time"
      }

      "should clear user answers and redirect to dashboard page when data has already been submitted" in new Test {
        override val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = JsObject(Seq(
            "submittedDate" -> Json.toJson(Instant.now()),
            "leppSummary" -> Json.toJson(summaryModel)
          ))
        )

        when(
          mockSessionService.save(userAnswers = ArgumentMatchers.any())(
            hc = ArgumentMatchers.any(),
            ec = ArgumentMatchers.any()
          )
        ).thenReturn(Future.successful(()))

        val result: Future[Result] = controller.handleWithBankDetails(
          req => _ => Future.successful(ImATeapot("teapot time"))
        )(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.ClearCacheController.onPageLoad().url)
      }
    }

    "handleForConfirmationPage" - {
      "should redirect to dashboard page when claims data isn't cached" in new Test {
        val result: Future[Result] = controller.handleForConfirmationPage(
          _ => _ => Future.successful(ImATeapot(""))
        )(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.DashboardController.onPageLoad().url)
      }

      "should redirect to bank details page when bank details aren't cached" in new Test {
        override val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = Json.obj("leppSummary" -> Json.toJson(summaryModel))
        )

        val result: Future[Result] = controller.handleForConfirmationPage(
          _ => _ => Future.successful(ImATeapot(""))
        )(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.WhatAreYourBankDetailsController.onPageLoad(NormalMode).url)
      }

      "should redirect to CYA page when data hasn't been submitted" in new Test {
        val detailsModel: BankAccountDetails = BankAccountDetails(
          accountName = "name nameson",
          accountNumber = "112233",
          sortCode = "12345678",
          rollNumber = None
        )

        override val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = Json.obj(
            "leppSummary" -> Json.toJson(summaryModel),
            "bankDetails" -> Json.toJson(detailsModel)
          )
        )

        val result: Future[Result] = controller.handleForConfirmationPage(
          _ => _ => Future.successful(ImATeapot("teapot time"))
        )(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.CheckYourAnswersController.onPageLoad().url)
      }

      "should evaluate block when all data is cached" in new Test {
        val detailsModel: BankAccountDetails = BankAccountDetails(
          accountName = "name nameson",
          accountNumber = "112233",
          sortCode = "12345678",
          rollNumber = None
        )

        override val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = Json.obj(
            "leppSummary" -> Json.toJson(summaryModel),
            "bankDetails" -> Json.toJson(detailsModel),
            "leppSubmissionSummary" -> Json.toJson(SubmissionSummary.empty)
          )
        )

        val result: Future[Result] = controller.handleForConfirmationPage(
          _ => _ => Future.successful(ImATeapot("teapot time"))
        )(FakeRequest())

        status(result) mustBe IM_A_TEAPOT
        contentAsString(result) mustBe "teapot time"
      }
    }
  }
}
