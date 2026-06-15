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

package connectors

import base.SpecBase
import config.AppConfig
import models.ResponseWrapper
import models.ResponseWrapper.{ErrorWrapper, SuccessWrapper}
import models.backend.retrieve.RetrieveLeppDetailsResponse
import models.errors.ErrorResult.ServiceErrorResult
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{GatewayTimeoutException, HeaderCarrier}
import utils.ErrorCodes.BAD_REQUEST_ERROR

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class LeppRetrievalConnectorSpec extends SpecBase {

  trait Test {
    type ServiceResult = DownstreamResponse[RetrieveLeppDetailsResponse]
    
    val mockConfig: AppConfig = mock[AppConfig]
    when(mockConfig.getPaymentsUrl).thenReturn("http://dummyUrl/nps")
    
    val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
    val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
    
    lazy val testConnector: LeppRetrievalConnector = new LeppRetrievalConnector(
      config = mockConfig,
      httpClient = mockHttpClient
    )

    lazy val connectorResponse: Future[ServiceResult] = Future.successful(
      Right(SuccessWrapper(retrieveResponse, testCorrelationId))
    )
    
    def setupStubs(): OngoingStubbing[Future[ServiceResult]] = {
      when(
        mockHttpClient.get(
          ArgumentMatchers.any[URL]()
        )(
          ArgumentMatchers.any[HeaderCarrier]()
        )
      ).thenReturn(mockRequestBuilder)

      when(mockRequestBuilder.setHeader(ArgumentMatchers.any())).thenReturn(mockRequestBuilder)

      when(
        mockRequestBuilder.execute[ServiceResult](
          ArgumentMatchers.eq(testConnector.httpReads),
          ArgumentMatchers.any()
        )
      ).thenReturn(connectorResponse)
    }

    implicit val correlationId: String = testCorrelationId.value
    lazy val requestOutcome: ConnectorResponse[RetrieveLeppDetailsResponse] = testConnector.retrieveLeppDetails()
  }
  
  "LeppRetrievalConnector" - {
    "retrieveLeppDetails" - {
      "should handle a success outcome" in new Test {
        setupStubs()
        val result: Either[ErrorWrapper, SuccessWrapper[RetrieveLeppDetailsResponse]] = await(requestOutcome.value)
        result mustBe a[Right[_, _]]
        val expectedResponse: ResponseWrapper[RetrieveLeppDetailsResponse] = SuccessWrapper(retrieveResponse, testCorrelationId)
        result.getOrElse(leppResponse) mustBe expectedResponse
      }

      "should handle any error response" in new Test {
        override lazy val connectorResponse: Future[ServiceResult] = Future.successful(
          Left(ErrorWrapper(ServiceErrorResult(BAD_REQUEST, BAD_REQUEST_ERROR), testCorrelationId))
        )
        setupStubs()

        val result: Either[ErrorWrapper, SuccessWrapper[RetrieveLeppDetailsResponse]] = await(requestOutcome.value)
        result mustBe a[Left[_, _]]

        val expectedError: ErrorWrapper = ErrorWrapper(
          value = ServiceErrorResult(BAD_REQUEST, BAD_REQUEST_ERROR),
          correlationId = testCorrelationId
        )
        result.swap.getOrElse(testServiceErrorWrapper) mustBe expectedError
      }

      "should handle a failed response" in new Test {
        override lazy val connectorResponse: Future[ServiceResult] = Future.failed(new GatewayTimeoutException(""))
        setupStubs()
        lazy val result: Either[ErrorWrapper, SuccessWrapper[RetrieveLeppDetailsResponse]] = await(requestOutcome.value)
        assertThrows[GatewayTimeoutException](result)
      }
    } 
  }
}
