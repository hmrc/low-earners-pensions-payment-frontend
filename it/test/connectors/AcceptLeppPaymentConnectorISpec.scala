package connectors

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.{IntegrationSpecBase, WireMockMethods}
import models.ResponseWrapper.{ErrorWrapper, SuccessWrapper}
import models.{CorrelationId, ResponseWrapper}
import models.backend.accept.{AcceptLeppPaymentRequest, AcceptLeppPaymentRequestBody, AcceptLeppPaymentResponse}
import models.errors.ErrorResult
import models.userAnswers.BankAccountDetails
import play.api.Application
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import utils.ErrorCodes.*

import scala.concurrent.ExecutionContext.Implicits.global

class AcceptLeppPaymentConnectorISpec extends IntegrationSpecBase with WireMockMethods {
  
  trait Test {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    
    val nino: String = generateNino()
    
    val requestBody: AcceptLeppPaymentRequestBody = AcceptLeppPaymentRequestBody(
      currentLowEarnersOptimisticLock = 1234,
      lowEarnersAccountDetails = BankAccountDetails(
        accountName = "Name",
        accountNumber = "12345678",
        sortCode = "123456",
        rollNumber = Some("ROLL")
      )
    )
    
    val request = AcceptLeppPaymentRequest(
      identifier = Nino(nino),
      taxYear = 2025,
      body = requestBody
    )
    
    lazy val application: Application = fakeApplication()
    lazy val connector: AcceptLeppPaymentConnector = application.injector.instanceOf[AcceptLeppPaymentConnector]
    
    lazy val futureResult: ConnectorResponse[AcceptLeppPaymentResponse] = connector.acceptPayment(request)
    
    def mockResult(status: Int, responseBody: String): StubMapping = when(
      method = POST,
      uri = "/low-earners-pensions-payment/accept-payment/2025", 
      headers = Map("CorrelationId" -> testCorrelationId),
      bodyOpt = Some(Json.toJson(requestBody).toString)
    ).thenReturn(status, responseBody)
  }
  
  "AcceptLeppPaymentConnector" when {
    "accept payment" should {
      def handleForErrors(errorStatus: Int, expectedStatus: Int, expectedCode: String): Unit = {
        s"should handle correctly for error status: $errorStatus" in new Test {
          mockResult(errorStatus, "")
          val result: Either[ErrorWrapper, SuccessWrapper[AcceptLeppPaymentResponse]] = await(futureResult.value)
          result shouldBe a[Left[_, _]]
          val error: ErrorResult = result.swap.getOrElse(dummyErrorWrapper).value
          error.status shouldBe expectedStatus
          error.code shouldBe expectedCode
          error.source shouldBe "BACKEND"
        }
      }

      Seq(
        (BAD_REQUEST, BAD_REQUEST, BAD_REQUEST_ERROR),
        (CONFLICT, CONFLICT, CONFLICT_ERROR),
        (INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR, INTERNAL_ERROR),
        (IM_A_TEAPOT, INTERNAL_SERVER_ERROR, UNEXPECTED_STATUS)
      ).foreach(handleForErrors)
      
      "handle correctly for success response" in new Test {
        mockResult(
          CREATED,
          """
            |{
            | "updatedLowEarnersOptimisticLock": 1234
            |}
          """.stripMargin
        )
        val result: Either[ErrorWrapper, SuccessWrapper[AcceptLeppPaymentResponse]] = await(futureResult.value)
        result shouldBe a[Right[_, _]]

        val dummyAcceptResponse: SuccessWrapper[AcceptLeppPaymentResponse] = SuccessWrapper[AcceptLeppPaymentResponse](
          value = AcceptLeppPaymentResponse(9999),
          correlationId = CorrelationId("DUMMY-ID")
        )
        
        val success: SuccessWrapper[AcceptLeppPaymentResponse] = result.getOrElse(dummyAcceptResponse)
        success.value shouldBe AcceptLeppPaymentResponse(1234)
      }
    }
  }
}
