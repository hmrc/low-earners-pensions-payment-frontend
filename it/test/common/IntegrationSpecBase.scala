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
import controllers.actions.IdentifierAction
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.{HeaderNames, Status}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.BodyParsers
import play.api.test.{DefaultAwaitTimeout, ResultExtractors}
import play.api.{Application, inject}
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}

import scala.util.Random

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

  override def fakeApplication(): Application = {
    GuiceApplicationBuilder()
      .configure(
        "microservice.services.bars.port" -> wireMockPort,
        "microservice.services.bars.host" -> wireMockHost,
        "microservice.services.bars.env"  -> "local",
        "microservice.services.auth.port" -> wireMockPort,
        "microservice.services.auth.host" -> wireMockHost
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

  def validNino(prefix: String = "AA"): String = {
    val num = Random.nextInt(1000000)
    val suffix = "A"
    val str: String = Random.alphanumeric.filter(_.isLetter).take(2).map(_.toUpper).mkString

    prefix + f"$str$num%06d$suffix".drop(prefix.length)
  }
}
