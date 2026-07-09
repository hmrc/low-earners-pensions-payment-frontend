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

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import base.IntegrationSpecBase
import models.CorrelationId
import models.barsLockout.{BarsVerifyStatusResponse, NumberOfBarsVerifyAttempts}
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.Application
import play.api.http.Status.*
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.HeaderCarrier
import utils.Constants

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global

class BarsVerifyStatusConnectorISpec extends IntegrationSpecBase {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val correlationId: CorrelationId = CorrelationId("X-123")
  val statusUrl = "/low-earners-pensions-payment/bars/verify/status"
  val updateUrl = "/low-earners-pensions-payment/bars/verify/update"
  
  trait Test {
    
    val app: Application = fakeApplication()

    val connector: BarsVerifyStatusConnector = app.injector.instanceOf[BarsVerifyStatusConnector]

    def setUpPost(status: Int, response: String): StubMapping = stubPost(
      updateUrl,
      JsObject.empty.toString,
      aResponse().withStatus(status).withBody(response).withHeader(Constants.correlationIdKey, "X-123")
    )

    def setUpGet(status: Int, response: String): StubMapping = stubGet(
      statusUrl,
      aResponse().withStatus(status).withBody(response).withHeader(Constants.correlationIdKey, "X-123")
    )
  }

  "verifyStatus" should {
    "return valid response with expiryTime" in new Test {

      val response: String =
        """
          |{
          | "attempts": 3,
          | "lockoutExpiryDateTime": "2020-12-26T00:00:00Z"
          |}""".stripMargin
      
      setUpGet(OK, response)

      val expected = BarsVerifyStatusResponse(NumberOfBarsVerifyAttempts(1), Some(Instant.parse("2020-12-26T00:00:00Z")))
      
      private val result = await(connector.status())
      Json.toJson(result) shouldBe Json.parse(response)
      WireMock.verify(getRequestedFor(urlEqualTo(statusUrl)))
    }

    "return valid response with no lockout time" in new Test {

      val response: String =
        """
          |{
          | "attempts": 1
          |}""".stripMargin

      setUpGet(OK, response)

      val expected = BarsVerifyStatusResponse(NumberOfBarsVerifyAttempts(1), None)

      private val result = await(connector.status())
      Json.toJson(result) shouldBe Json.parse(response)
      WireMock.verify(getRequestedFor(urlEqualTo(statusUrl)))
    }
  }
  
  "updateStatus" should {
    "return valid response with expiryTime for 3 or more attempts" in new Test {

      val response: String =
        """
          |{
          | "attempts": 3,
          | "lockoutExpiryDateTime": "2020-12-26T00:00:00Z"
          |}""".stripMargin

      setUpPost(OK, response)

      val expected = BarsVerifyStatusResponse(NumberOfBarsVerifyAttempts(1), Some(Instant.parse("2020-12-26T00:00:00Z")))

      private val result = await(connector.update())
      Json.toJson(result) shouldBe Json.parse(response)
      WireMock.verify(postRequestedFor(urlEqualTo(updateUrl)))
    }

    "return valid response with no lockout time" in new Test {

      val response: String =
        """
          |{
          | "attempts": 1
          |}""".stripMargin

      setUpPost(OK, response)

      val expected = BarsVerifyStatusResponse(NumberOfBarsVerifyAttempts(1), None)

      private val result = await(connector.update())
      Json.toJson(result) shouldBe Json.parse(response)
      WireMock.verify(postRequestedFor(urlEqualTo(updateUrl)))
    }
  } 
}
