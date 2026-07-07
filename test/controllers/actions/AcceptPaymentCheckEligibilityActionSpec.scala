package controllers.actions

import base.SpecBase
import models.requests.{AuthUser, DataRequest, EligibleDataRequest}
import models.userAnswers.{LeppSummary, UserAnswers}
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Result, Results}
import play.api.test.FakeRequest

import scala.concurrent.ExecutionContext.Implicits.global

class AcceptPaymentCheckEligibilityActionSpec extends SpecBase with Results {
  private val testAction: AcceptPaymentCheckEligibilityAction = new AcceptPaymentCheckEligibilityAction
  private val user: AuthUser = AuthUser(userId = "id", nino = generateNino(), itmpNameOpt = None)
  
  "AcceptPaymentCheckEligibilityAction" - {
    "refine" - {
      "should redirect to Dashboard page when there is no LeppSummary in user answers" in {
        val request = DataRequest[AnyContent](
          request = FakeRequest(),
          user = user,
          userAnswers = UserAnswers("id")
        )
        
        val result: Either[Result, EligibleDataRequest[AnyContent]] = await(
          testAction.refine(request)
        )
        
        result mustBe a[Left[_, _]]
        result.swap.getOrElse(ImATeapot("")) mustBe Redirect(controllers.routes.DashboardController.onPageLoad())
      }

      "should return EligibleDataRequest when LeppSummary exists in user answers" in {
        val request: DataRequest[AnyContent] = DataRequest[AnyContent](
          request = FakeRequest(),
          user = user,
          userAnswers = UserAnswers(
            id = "id",
            data = Json.obj("leppSummary" -> Json.toJson(summaryModel))
          )
        )

        val result: Either[Result, EligibleDataRequest[AnyContent]] = await(
          testAction.refine(request)
        )

        val dummyRequest: EligibleDataRequest[AnyContentAsEmpty.type] = EligibleDataRequest(
          request = DataRequest(
            request = FakeRequest(),
            user = user,
            userAnswers = UserAnswers("DUMMY_ID")
          ),
          leppSummary = LeppSummary(currentLock = 123)
        )

        result mustBe a[Right[_, _]]
        result.getOrElse(dummyRequest) mustBe EligibleDataRequest(request = request, leppSummary = summaryModel)
      }
    }
  }
}
