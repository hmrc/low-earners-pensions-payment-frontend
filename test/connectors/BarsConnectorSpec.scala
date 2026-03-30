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
import models.bars.{BarsResponse, ValidatedBarsRequest}
import models.errors.ErrorResult.BarsErrorResult
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{GatewayTimeoutException, HeaderCarrier}

import java.net.URL
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class BarsConnectorSpec extends SpecBase {
  trait Test {
    type BarsResult = DownstreamResponse[BarsResponse]
    
    val mockConfig: AppConfig = mock[AppConfig]
    when(mockConfig.barsUrl).thenReturn("http://dummyUrl/bars")
    
    val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
    val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
    
    lazy val testConnector: BarsConnector = new BarsConnector(
      config = mockConfig,
      httpClient = mockHttpClient
    )
    
    val testRequest: ValidatedBarsRequest = testValidatedBarsRequest
    
    lazy val connectorResponse: Future[BarsResult] = Future.successful(
      Right(SuccessWrapper(testBarsResponse, testCorrelationId))
    )
    
    def setupStubs(): OngoingStubbing[Future[BarsResult]] = {
      when(
        mockHttpClient.post(
          ArgumentMatchers.any[URL]()
        )(
          ArgumentMatchers.any[HeaderCarrier]()
        )
      ).thenReturn(mockRequestBuilder)
      
      when(
        mockRequestBuilder.withBody(
          ArgumentMatchers.any[JsValue]()
        )(
          ArgumentMatchers.any(),
          ArgumentMatchers.any(),
          ArgumentMatchers.any()
        )
      ).thenReturn(mockRequestBuilder)

      when(mockRequestBuilder.setHeader(ArgumentMatchers.any())).thenReturn(mockRequestBuilder)

      when(
        mockRequestBuilder.execute[BarsResult](
          ArgumentMatchers.eq(testConnector.httpReads),
          ArgumentMatchers.any()
        )
      ).thenReturn(connectorResponse)
    }

    lazy val requestOutcome: ConnectorResponse[BarsResponse] = testConnector.checkBankAccountDetails(
      request = testRequest,
      correlationId = testCorrelationId
    )
  }
  
  "checkBankAccountDetails" - {
    "should handle a success outcome" in new Test {
      setupStubs()
      val result: Either[ErrorWrapper, SuccessWrapper[BarsResponse]] = await(requestOutcome.value)
      result mustBe a[Right[_, _]]
      val expectedResponse: ResponseWrapper[BarsResponse] = SuccessWrapper(testBarsResponse, testCorrelationId)
      result.getOrElse(dummyBarsResponse) mustBe expectedResponse
    }

    "should handle any error response" in new Test {
      override lazy val connectorResponse: Future[BarsResult] = Future.successful(
        Left(ErrorWrapper(BarsErrorResult(IM_A_TEAPOT, "TEAPOT_TIME"), testCorrelationId))
      )
      setupStubs()
      
      val result: Either[ErrorWrapper, SuccessWrapper[BarsResponse]] = await(requestOutcome.value)
      result mustBe a[Left[_, _]]
      
      val expectedError: ErrorWrapper = ErrorWrapper(
        value = BarsErrorResult(IM_A_TEAPOT, "TEAPOT_TIME"),
        correlationId = testCorrelationId
      )
      result.swap.getOrElse(testDownstreamErrorWrapper) mustBe expectedError
    }

    "should handle a failed response" in new Test {
      override lazy val connectorResponse: Future[BarsResult] = Future.failed(new GatewayTimeoutException(""))
      setupStubs()
      lazy val result: Either[ErrorWrapper, SuccessWrapper[BarsResponse]] = await(requestOutcome.value)
      assertThrows[GatewayTimeoutException](result)
    }
  }
}
