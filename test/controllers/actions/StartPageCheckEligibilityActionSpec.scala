package controllers.actions

import base.SpecBase
import models.CorrelationId
import models.requests.{AuthUser, DataRequest, EligibleDataRequest}
import models.userAnswers.{LeppSummary, UserAnswers}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{never, verify, when}
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
    val mockSessionService: SessionCacheService = mock[SessionCacheService]
    val mockCorrelationIdHandler: CorrelationIdHandler = mock[CorrelationIdHandler]
    val mockLeppRetrievalService: LeppRetrievalService = mock[LeppRetrievalService]

    val user: AuthUser = AuthUser(userId = "id", nino = generateNino(), itmpNameOpt = None)

    lazy val request: DataRequest[AnyContent] = DataRequest[AnyContent](
      request = FakeRequest(),
      user = user,
      userAnswers = UserAnswers(
        id = "id",
        data = Json.obj("leppSummary" -> Json.toJson(summaryModel))
      )
    )
    
    val testObject: StartPageCheckEligibilityAction = new StartPageCheckEligibilityAction(withCaching = true)(
      sessionDataService = mockSessionService,
      correlationIdHandler = mockCorrelationIdHandler,
      leppRetrievalService = mockLeppRetrievalService
    )
    
    when(
      mockSessionService.clear(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
    ).thenReturn(Future.successful(true))
    
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

    lazy val result: Either[Result, EligibleDataRequest[AnyContent]] = await(testObject.refine(request))
  }
  
  "StartPageCheckEligibilityAction" - {
    "refine" - {
      "should redirect to NotEligible page when user has no LEPP data" in new Test {
        mockLeppRetrieval(None)
        
        result mustBe a[Left[_, _]]
        result.swap.getOrElse(ImATeapot("")) mustBe Redirect(controllers.auth.routes.IneligibleController.onPageLoad())
      }
      
      "should redirect to NotEligible page when user has no applicable LEPP data" in new Test {
        mockLeppRetrieval(Some(LeppSummary(currentLock = 1)))

        result mustBe a[Left[_, _]]
        result.swap.getOrElse(ImATeapot("")) mustBe Redirect(controllers.auth.routes.IneligibleController.onPageLoad())
      }

      "should return EligibleDataRequest if LEPP exists" in new Test {
        mockLeppRetrieval(Some(summaryModel))
        
        val dummyRequest: EligibleDataRequest[AnyContent] = EligibleDataRequest(
          request = DataRequest(
            request = FakeRequest(),
            user = user,
            userAnswers = UserAnswers("DUMMY_ID")
          ),
          leppSummary = LeppSummary(currentLock = 123)
        )
        
        result mustBe a[Right[_, _]]
        val resultingRequest: EligibleDataRequest[AnyContent] = result.getOrElse(dummyRequest)
        resultingRequest.user mustBe request.user
        resultingRequest.leppSummary mustBe summaryModel
      }
      
      "should not cache data if flag is set to false" in new Test {
        override val testObject: StartPageCheckEligibilityAction = new StartPageCheckEligibilityAction(withCaching = false)(
          sessionDataService = mockSessionService,
          correlationIdHandler = mockCorrelationIdHandler,
          leppRetrievalService = mockLeppRetrievalService
        )

        mockLeppRetrieval(Some(summaryModel))

        result mustBe a[Right[_, _]]
        verify(mockSessionService, never()).save(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
      }
    }
  }
}
