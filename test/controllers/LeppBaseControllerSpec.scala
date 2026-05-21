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
import controllers.actions.{DataRetrievalAction, FakeDataRetrievalAction, FakeIdentifierAction, IdentifierAction}
import models.userAnswers.LeppItemStatus.{Available, Cancelled, Paid, Suspended}
import models.userAnswers.{BankAccountDetails, LeppItem, LeppSummary, UserAnswers}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import pages.*
import play.api.libs.json.{JsBoolean, JsObject, JsString, Json}
import play.api.mvc.Results.ImATeapot
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesControllerComponents
import services.SessionCacheService
import viewmodels.{CheckMode, Mode, NormalMode}

import scala.concurrent.{ExecutionContext, Future}

class LeppBaseControllerSpec extends SpecBase {
  private trait Test {
    val userAnswers: UserAnswers = UserAnswers("1")
    val mockAuth: IdentifierAction = FakeIdentifierAction()
    val mockSessionService: SessionCacheService = mock[SessionCacheService]
    private val mockCc: MessagesControllerComponents = stubMessagesControllerComponents()
    private lazy val mockData: DataRetrievalAction = FakeDataRetrievalAction(userAnswers)

    val summaryModel: LeppSummary = LeppSummary(
      currentLock = 67,
      availableItems = Some(Seq(
        LeppItem(
          id = "A-25-1",
          taxYear = 2025,
          contributions = 1000,
          taxRate = 20,
          entitlement = 200,
          status = Available,
          claimDate = None
        )
      )),
      paidItems = Some(Seq(
        LeppItem(
          id = "P-25-1",
          taxYear = 2025,
          contributions = 1000,
          taxRate = 20,
          entitlement = 200,
          status = Paid,
          claimDate = None
        )
      )),
      suspendedItems = Some(Seq(
        LeppItem(
          id = "S-25-1",
          taxYear = 2025,
          contributions = 1000,
          taxRate = 20,
          entitlement = 200,
          status = Suspended,
          claimDate = None
        )
      )),
      cancelledItems = Some(Seq(
        LeppItem(
          id = "C-25-1",
          taxYear = 2025,
          contributions = 1000,
          taxRate = 20,
          entitlement = 200,
          status = Cancelled,
          claimDate = None
        )
      ))
    )
    
    class DummyController(id: IdentifierAction,
                          data: DataRetrievalAction,
                          val controllerComponents: MessagesControllerComponents = mockCc)
      extends LeppBaseController(id, data) with SessionDataHandling {
      
      override val sessionService: SessionCacheService = mockSessionService
      override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    }
    
    lazy val controller: DummyController = new DummyController(mockAuth, mockData)
  }
  
  "LeppBaseController" - {
    "submitUrl" - {
      def testSubmitUrl(mode: Mode, page: Page, link: String): Unit =
        s"should return correct submit link for page: $page, and mode: $mode" in new Test {
          controller.submitUrl(mode, page).url mustBe link
        }

      Seq(
        (NormalMode, WhatAreYourBankDetailsPage, "/low-earners-pensions-payment/bank-details"),
        (CheckMode,  WhatAreYourBankDetailsPage, "/low-earners-pensions-payment/change-bank-details"),
        (NormalMode, CheckYourAnswersPage, "/low-earners-pensions-payment/check-your-answers"),
        (NormalMode, ConfirmationPage, "/low-earners-pensions-payment/dashboard")
      ).foreach(testSubmitUrl)
    }
    
    "backLinkUrl" - {
      def testBackLink(mode: Mode, page: Page, link: String): Unit =
        s"should return correct back link for page: ${page.toString}, and mode: $mode" in new Test {
        controller.backLinkUrl(mode, page).url mustBe link
      }

      Seq(
        (NormalMode, DashboardPage, "/low-earners-pensions-payment/start"),
        (NormalMode, PaymentCalcBreakdownPage, "/low-earners-pensions-payment/dashboard"),
        (NormalMode, WhatAreYourBankDetailsPage, "/low-earners-pensions-payment/breakdown"),
        (CheckMode,  WhatAreYourBankDetailsPage, "/low-earners-pensions-payment/check-your-answers"),
        (NormalMode, CheckYourAnswersPage, "/low-earners-pensions-payment/bank-details"),
        (NormalMode, ConfirmationPage, "/low-earners-pensions-payment/dashboard")
      ).foreach(testBackLink)
    }
    
    "handle" - {
      "should invoke block when authorisation succeeds" in new Test {
        val result: Future[Result] = controller.handle(
          _ => Future.successful(ImATeapot("Teapot time"))
        )(FakeRequest())
        
        status(result) mustBe IM_A_TEAPOT
        contentAsString(result) mustBe "Teapot time"
      }

      "should not invoke block when authorisation fails" in new Test {
        override val mockAuth = FakeIdentifierAction(true)
        
        val result: Future[Result] = controller.handle(
          _ => Future.successful(ImATeapot("Teapot time"))
        )(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("some-url")
      }
    }
  }
  
  "SessionDataHandling" - {
    "handleWithSubmissionCheck" - {
      "should wipe user answers when data has been submitted" in new Test {
        when(
          mockSessionService.save(userAnswers = ArgumentMatchers.any())(
            hc = ArgumentMatchers.any(),
            ec = ArgumentMatchers.any()
          )
        ).thenReturn(Future.successful(()))
        
        override val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = JsObject(Seq("isSubmitted" -> JsBoolean(true)))
        )
        
        lazy val result: Future[Result] = controller.handleWithSubmissionCheck(
          req => Future.successful(ImATeapot(req.userAnswers.data))
        )(FakeRequest())
        
        status(result) mustBe IM_A_TEAPOT
        contentAsJson(result) mustBe JsObject.empty
      }

      "should not wipe user answers when data has not been submitted" in new Test {
        when(
          mockSessionService.save(
            ArgumentMatchers.any()
          )(
            ArgumentMatchers.any(),
            ArgumentMatchers.any()
          )
        ).thenReturn(
          Future.successful(())
        )

        override val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = JsObject(Seq("otherData" -> JsString("value")))
        )

        lazy val result: Future[Result] = controller.handleWithSubmissionCheck(
          req => Future.successful(ImATeapot(req.userAnswers.data))
        )(FakeRequest())

        status(result) mustBe IM_A_TEAPOT
        contentAsJson(result) mustBe Json.parse("""{"otherData": "value"}""")
      }
    }
    
    "handleWithLeppData" - {
      "should redirect to dashboard page when claims data isn't cached" in new Test {
        val result: Future[Result] = controller.handleWithLeppData(
          _ => _ => Future.successful(ImATeapot(""))
        )(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.DashboardController.onPageLoad().url)
      }

      "should evaluate block when LEPP data is cached" in new Test {
        override val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = Json.obj("leppSummary" -> Json.toJson(summaryModel))
        )

        val result: Future[Result] = controller.handleWithLeppData(
          _ => _ => Future.successful(ImATeapot("teapot time"))
        )(FakeRequest())

        status(result) mustBe IM_A_TEAPOT
        contentAsString(result) mustBe "teapot time"
      }

      "should clear user answers and redirect to dashboard page when data has already been submitted" in new Test {
        override val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = JsObject(Seq(
            "isSubmitted" -> JsBoolean(true),
            "leppSummary" -> Json.toJson(summaryModel)
          ))
        )

        when(
          mockSessionService.save(userAnswers = ArgumentMatchers.any())(
            hc = ArgumentMatchers.any(),
            ec = ArgumentMatchers.any()
          )
        ).thenReturn(Future.successful(()))
        
        val result: Future[Result] = controller.handleWithLeppData(
          req => _ => Future.successful(ImATeapot("teapot time"))
        )(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.DashboardController.onPageLoad().url)
      }
    }
    
    "handleWithBankDetails" - {
      "should redirect to dashboard page when claims data isn't cached" in new Test {
        val result: Future[Result] = controller.handleWithBankDetails(
          _ => _ => Future.successful(ImATeapot(""))
        )(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.DashboardController.onPageLoad().url)
      }

      "should redirect to bank details page when bank details aren't cached" in new Test {
        override val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = Json.obj("leppSummary" -> Json.toJson(summaryModel))
        )

        val result: Future[Result] = controller.handleWithBankDetails(
          _ => _ => Future.successful(ImATeapot(""))
        )(FakeRequest())

        println(userAnswers.get(DashboardPage))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.WhatAreYourBankDetailsController.onPageLoad(NormalMode).url)
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

        val result: Future[Result] = controller.handleWithLeppData(
          _ => _ => Future.successful(ImATeapot("teapot time"))
        )(FakeRequest())

        status(result) mustBe IM_A_TEAPOT
        contentAsString(result) mustBe "teapot time"
      }

      "should clear user answers and redirect to dashboard page when data has already been submitted" in new Test {
        override val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = JsObject(Seq(
            "isSubmitted" -> JsBoolean(true),
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
        redirectLocation(result) mustBe Some(routes.DashboardController.onPageLoad().url)
      }
    }
    
    "handleForConfirmationPage" - {
      "should redirect to dashboard page when claims data isn't cached" in new Test {
        val result: Future[Result] = controller.handleForConfirmationPage(
          _ => Future.successful(ImATeapot(""))
        )(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.DashboardController.onPageLoad().url)
      }

      "should redirect to bank details page when bank details aren't cached" in new Test {
        override val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = Json.obj("leppSummary" -> Json.toJson(summaryModel))
        )

        val result: Future[Result] = controller.handleForConfirmationPage(
          _ => Future.successful(ImATeapot(""))
        )(FakeRequest())

        println(userAnswers.get(DashboardPage))

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.WhatAreYourBankDetailsController.onPageLoad(NormalMode).url)
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
          _ => Future.successful(ImATeapot("teapot time"))
        )(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.CheckYourAnswersController.onPageLoad().url)
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
            "isSubmitted" -> JsBoolean(true)
          )
        )

        val result: Future[Result] = controller.handleForConfirmationPage(
          _ => Future.successful(ImATeapot("teapot time"))
        )(FakeRequest())

        status(result) mustBe IM_A_TEAPOT
        contentAsString(result) mustBe "teapot time"
      }
    }
  }
}
