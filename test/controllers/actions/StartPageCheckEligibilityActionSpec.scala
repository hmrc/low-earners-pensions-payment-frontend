package controllers.actions

import base.SpecBase
import models.CorrelationId
import models.requests.{AuthUser, DataRequest, EligibleDataRequest}
import models.userAnswers.{LeppSummary, UserAnswers}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Result, Results}
import play.api.test.FakeRequest
import services.{LeppRetrievalService, SessionCacheService}
import utils.CorrelationIdHandler
import connectors.{ConnectorResponse, rawConnectorFailure, rawConnectorSuccess}
import models.errors.ErrorResult.BackendErrorResult
import org.mockito.stubbing.OngoingStubbing

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StartPageCheckEligibilityActionSpec extends SpecBase with Results {
  
  private trait Test {
    private val mockSessionService: SessionCacheService = mock[SessionCacheService]
    private val mockCorrelationIdHandler: CorrelationIdHandler = mock[CorrelationIdHandler]
    private val mockLeppRetrievalService: LeppRetrievalService = mock[LeppRetrievalService]

    val user: AuthUser = AuthUser(userId = "id", nino = generateNino(), itmpNameOpt = None)

    val request: DataRequest[AnyContent] = DataRequest[AnyContent](
      request = FakeRequest(),
      user = user,
      userAnswers = UserAnswers(
        id = "id",
        data = Json.obj("leppSummary" -> Json.toJson(summaryModel))
      )
    )
    
    val testObject: StartPageCheckEligibilityAction = new StartPageCheckEligibilityAction(
      sessionDataService = mockSessionService,
      correlationIdHandler = mockCorrelationIdHandler,
      leppRetrievalService = mockLeppRetrievalService
    )
    
    when(
      mockCorrelationIdHandler.getCorrelationId(ArgumentMatchers.any())
    ).thenReturn(CorrelationId("ID"))
    
    when(
      mockSessionService.save(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
    ).thenReturn(
      Future.successful(())
    )
    
    def mockLeppRetrieval(dataOpt: Option[LeppSummary]): OngoingStubbing[ConnectorResponse[LeppSummary]] = when(
      mockLeppRetrievalService.retrieveLeppDetails()(
        hc = ArgumentMatchers.any(),
        ec = ArgumentMatchers.any(),
        cid = ArgumentMatchers.any()
      )
    ).thenReturn(
      dataOpt.fold(
        rawConnectorFailure(BackendErrorResult(IM_A_TEAPOT, "ERROR"))
      )(
        data => rawConnectorSuccess(data)
      )
    )
  }
  
  "StartPageCheckEligibilityAction" - {
    "refine" - {
      "should redirect to NotEligible page when user has no LEPP data" in new Test {
        mockLeppRetrieval(None)
        val result: Either[Result, EligibleDataRequest[AnyContent]] = await(testObject.refine(request))
        
        result mustBe a[Left[_, _]]
        result.swap.getOrElse(ImATeapot("")) mustBe Redirect(controllers.auth.routes.IneligibleController.onPageLoad())
      }
      
      "should redirect to NotEligible page when user has no applicable LEPP data" in new Test {
        mockLeppRetrieval(Some(LeppSummary(currentLock = 1)))
        val result: Either[Result, EligibleDataRequest[AnyContent]] = await(testObject.refine(request))

        result mustBe a[Left[_, _]]
        result.swap.getOrElse(ImATeapot("")) mustBe Redirect(controllers.auth.routes.IneligibleController.onPageLoad())
      }

      "should return EligibleDataRequest if LEPP exists" in new Test {
        mockLeppRetrieval(Some(summaryModel))
        val result: Either[Result, EligibleDataRequest[AnyContent]] = await(testObject.refine(request))

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
