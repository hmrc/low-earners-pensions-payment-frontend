package controllers

import common.IntegrationSpecBase
import play.api.Application
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.writeableOf_AnyContentAsEmpty
import uk.gov.hmrc.http.SessionKeys
import play.api.test.Helpers.*

import scala.concurrent.Future

class BarsCheckFailedControllerIntegrationSpec extends ControllerIntegrationSpecBase {
  "GET /bank-details-check-failed" when {
    val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
      method = "GET",
      path = "/low-earners-pensions-payment/bank-details-check-failed"
    ).withSession(SessionKeys.authToken -> "auth token")
    
    testControllerAuth(request)

    "should return the expected view for a successful request" in {
      mockAuthSuccess()

      lazy val application: Application = fakeApplication()

      lazy val result: Future[Result] = route(application, request).getOrElse(
        Future.failed(new RuntimeException("TEST_ERROR"))
      )
      
      status(result) shouldBe INTERNAL_SERVER_ERROR
      contentAsString(result) should include("Please try again later")
    }
  }
}
