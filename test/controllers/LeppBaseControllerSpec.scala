package controllers

import base.SpecBase
import controllers.actions.{DataRetrievalAction, FakeDataRetrievalAction, FakeIdentifierAction, IdentifierAction}
import models.userAnswers.{BankAccountDetails, ClaimItem, ClaimsSummary, UserAnswers}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import pages.*
import pages.TempPage.Dashboard
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.api.test.Helpers.stubMessagesControllerComponents
import viewmodels.{CheckMode, Mode, NormalMode}
import play.api.mvc.Results.ImATeapot
import play.api.test.FakeRequest

import scala.concurrent.Future

class LeppBaseControllerSpec extends SpecBase {
  private trait Test {
    val userAnswers: UserAnswers = UserAnswers("1")
    private val mockAuth: IdentifierAction = FakeIdentifierAction()
    private val mockCc: MessagesControllerComponents = stubMessagesControllerComponents()
    private lazy val mockData: DataRetrievalAction = FakeDataRetrievalAction(userAnswers)
    
    class DummyController(id: IdentifierAction, data: DataRetrievalAction) extends LeppBaseController(id, data) {
      val controllerComponents: MessagesControllerComponents = mockCc
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
        (NormalMode, DashboardPage, "/low-earners-pensions-payment/dashboard"),
        (NormalMode, BreakdownPage, "/low-earners-pensions-payment/breakdown"),
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
        (NormalMode, BreakdownPage, "/low-earners-pensions-payment/dashboard"),
        (NormalMode, WhatAreYourBankDetailsPage, "/low-earners-pensions-payment/breakdown"),
        (CheckMode,  WhatAreYourBankDetailsPage, "/low-earners-pensions-payment/check-your-answers"),
        (NormalMode, CheckYourAnswersPage, "/low-earners-pensions-payment/bank-details"),
        (NormalMode, ConfirmationPage, "/low-earners-pensions-payment/dashboard")
      ).foreach(testBackLink)
    }
    
    "handleWithData" - {
      "should redirect to dashboard page when claims data isn't cached" in new Test {
        val result: Future[Result] = controller.handleWithData(
          _ => _ => Future.successful(ImATeapot(""))
        )(FakeRequest())
        
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.TempLeppController.onPageLoad(Dashboard).url)
      }

      "should evaluate block when all data is cached" in new Test {
        val summaryModel: ClaimsSummary = ClaimsSummary(
          currentLock = 67,
          claims = Seq(
            ClaimItem(
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              claimed = false
            )
          )
        )
        
        override val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = Json.obj("claimsSummary" -> Json.toJson(summaryModel))
        )
        
        val result: Future[Result] = controller.handleWithData(
          _ => _ => Future.successful(ImATeapot("teapot time"))
        )(FakeRequest())
        
        status(result) mustBe IM_A_TEAPOT
        contentAsString(result) mustBe "teapot time"
      }
    }
    
    "handleWithDataAndDetails" - {
      "should redirect to dashboard page when claims data isn't cached" in new Test {
        val result: Future[Result] = controller.handleWithDataAndDetails(
          _ => _ => Future.successful(ImATeapot(""))
        )(FakeRequest())
        
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.TempLeppController.onPageLoad(Dashboard).url)
      }
      
      "should redirect to bank details page when bank details aren't cached" in new Test {
        val summaryModel: ClaimsSummary = ClaimsSummary(
          currentLock = 67,
          claims = Seq(
            ClaimItem(
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              claimed = false
            )
          )
        )

        override val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = Json.obj("claimsSummary" -> Json.toJson(summaryModel))
        )
        
        val result: Future[Result] = controller.handleWithDataAndDetails(
          _ => _ => Future.successful(ImATeapot(""))
        )(FakeRequest())
        
        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.WhatAreYourBankDetailsController.onPageLoad(NormalMode).url)
      }

      "should evaluate block when all data is cached" in new Test {
        val summaryModel: ClaimsSummary = ClaimsSummary(
          currentLock = 67,
          claims = Seq(
            ClaimItem(
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              claimed = false
            )
          )
        )
        
        val detailsModel: BankAccountDetails = BankAccountDetails(
          name = "name nameson",
          accountNumber = "112233",
          sortCode = "12345678",
          rollNumber = None
        )

        override val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = Json.obj(
            "claimsSummary" -> Json.toJson(summaryModel),
            "bankDetails" -> Json.toJson(detailsModel)
          )
        )

        val result: Future[Result] = controller.handleWithData(
          _ => _ => Future.successful(ImATeapot("teapot time"))
        )(FakeRequest())

        status(result) mustBe IM_A_TEAPOT
        contentAsString(result) mustBe "teapot time"
      }
    }
  }
  
}
