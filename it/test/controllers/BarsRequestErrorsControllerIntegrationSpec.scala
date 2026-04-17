package controllers

import common.IntegrationSpecBase
import play.api.Application
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.writeableOf_AnyContentAsEmpty
import uk.gov.hmrc.http.SessionKeys
import play.api.test.Helpers.*
import views.html.bars.BarsRequestErrorsView

import scala.concurrent.Future

  class BarsRequestErrorsControllerIntegrationSpec extends ControllerIntegrationSpecBase {
    "GET /bank-details-check-errors" when {
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
        method = "GET",
        path = "/low-earners-pensions-payment/bank-details-check-errors"
      ).withSession(SessionKeys.authToken -> "auth token")
      
      testControllerAuth(request)
      
      "should return the expected view for a successful request" in {
        mockAuthSuccess()
        
        lazy val application: Application = fakeApplication()

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("If this issue persists you may have to contact HMRC")
      }
    }
}
