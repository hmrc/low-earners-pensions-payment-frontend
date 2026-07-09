package controllers.common

import base.SpecBase
import controllers.actions.fakes.{FakeDataRetrievalAction, FakeIdentifierAction, FakeStartPageCheckEligibilityAction, FakeStartPageCheckEligibilityActionBuilder}
import controllers.actions.{CheckEligibilityAction, DataRetrievalAction, IdentifierAction}
import models.userAnswers.{LeppSummary, UserAnswers}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.{JsBoolean, JsObject, JsString, Json}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.mvc.Results.ImATeapot
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesControllerComponents

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class EligibleLeppBaseControllerSpec extends SpecBase {
  trait Test {
    val userAnswers: UserAnswers = UserAnswers("1")
    val mockAuth: IdentifierAction = FakeIdentifierAction(nino = nino)
    
    lazy val mockGetData: DataRetrievalAction = FakeDataRetrievalAction(
      dataToReturn = userAnswers,
      lockoutTimestampOpt = None
    )
    
    val eligibilityResult: Right[Unit, LeppSummary] = Right(summaryModel)
    
    lazy val mockCheckEligibility: CheckEligibilityAction = FakeStartPageCheckEligibilityActionBuilder(eligibilityResult)(
      using mockSessionService, mockCidHandler, mockRetrievalService
    ).create(withCaching = true)
    
    class TestClass(identifierAction: IdentifierAction,
                    retrievalAction: DataRetrievalAction,
                    checkEligibilityAction: CheckEligibilityAction) extends EligibleLeppBaseController(
      identify = identifierAction,
      getData = retrievalAction,
      checkEligibility = checkEligibilityAction
    ) {

      override protected def controllerComponents: MessagesControllerComponents = stubMessagesControllerComponents()
    }
    
    lazy val controller: TestClass = new TestClass(mockAuth, mockGetData, mockCheckEligibility)
  }
  
  "EligibleLeppBaseController" - {
    "handleWithSubmissionCheck" - {
      "should redirect to clear cache controller when data has already submitted" in new Test {
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

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.ClearCacheController.onPageLoad().url)
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
  }
}
