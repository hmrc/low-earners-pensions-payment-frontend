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

package common

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import connectors.{BarsVerifyStatusConnector, PlaceholderBackendConnector}
import controllers.actions.{DataRetrievalAction, FakeDataRetrievalAction}
import models.userAnswers.UserAnswers
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.{HeaderNames, Status}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.BodyParsers
import play.api.test.{DefaultAwaitTimeout, ResultExtractors}
import play.api.{Application, inject}
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import utils.DateTime

import java.time.{Instant, ZoneId, ZonedDateTime}

class IntegrationSpecBase extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with DefaultAwaitTimeout
  with GuiceOneServerPerSuite
  with WireMockSupport 
  with ResultExtractors
  with HeaderNames
  with Status
  with HttpClientV2Support {

  val parsers: BodyParsers.Default = app.injector.instanceOf[BodyParsers.Default]
  
  class FakeDateTime extends DateTime {
    override def now(zoneId: ZoneId): ZonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(10000L), zoneId)
  }
  
  // TODO - This should be replaced by stubbing out the actual HTTP call to the backend once the connection is implemented
  val fakeConnector: PlaceholderBackendConnector = mock[PlaceholderBackendConnector]
  val fakeBarsVerifyStatusConnector: BarsVerifyStatusConnector = mock[BarsVerifyStatusConnector]

  override def fakeApplication(): Application = {
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.bars.port" -> wireMockPort,
        "microservice.services.bars.host" -> wireMockHost,
        "microservice.services.bars.env"  -> "local",
        "microservice.services.auth.port" -> wireMockPort,
        "microservice.services.auth.host" -> wireMockHost,
        "microservice.services.lepp-backend.port" -> wireMockPort,
        "microservice.services.lepp-backend.host" -> wireMockHost
      )
      .build()
  }

  def applicationWithUserAnswers(answers: UserAnswers, time: ZonedDateTime = ZonedDateTime.now()): Application = {
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.bars.port" -> wireMockPort,
        "microservice.services.bars.host" -> wireMockHost,
        "microservice.services.bars.env" -> "local",
        "microservice.services.auth.port" -> wireMockPort,
        "microservice.services.auth.host" -> wireMockHost,
        "microservice.services.lepp-backend.port" -> wireMockPort,
        "microservice.services.lepp-backend.host" -> wireMockHost
      )
      .overrides(
        bind[DateTime].toInstance(new FakeDateTime()),
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(answers)),
        bind[PlaceholderBackendConnector].toInstance(fakeConnector)
      )
      .build()
  }

  def stubPost(url: String, requestBody: String, response: ResponseDefinitionBuilder): StubMapping =
    wireMockServer.stubFor(
      post(urlEqualTo(url))
        .withHeader("Content-Type", equalTo("application/json"))
        .withRequestBody(equalTo(requestBody))
        .willReturn(response)
    )

  def stubGet(url: String, response: ResponseDefinitionBuilder): StubMapping =
    wireMockServer.stubFor(
      get(urlEqualTo(url))
        .willReturn(response)
    )
}
