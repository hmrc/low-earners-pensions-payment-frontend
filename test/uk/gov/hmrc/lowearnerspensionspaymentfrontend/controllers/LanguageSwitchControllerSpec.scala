/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.lowearnerspensionspaymentfrontend.controllers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class LanguageSwitchControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "play.i18n.langs" -> Seq("en", "cy")
      )
      .build()

  private val fakeRequest = FakeRequest("GET", "/").withCookies(Cookie("PLAY_LANG", "test"))
  private val fakeRequestWithReferer = fakeRequest.withHeaders(REFERER -> "/")
  private val controller = app.injector.instanceOf[LanguageSwitchController]

  "switchToLanguage" when {
    "a valid & supported language is supplied" should {
      "change lang & redirect to referring URL when it is defined" in {
        val result = controller.switchToLanguage("cy")(fakeRequestWithReferer)
        cookies(result).get("PLAY_LANG").map(_.value) shouldBe Some("cy")
        redirectLocation(result) shouldBe Some("/")
      }

      "change lang & redirect to fallback URL when no referring URL is defined" in {
        val result = controller.switchToLanguage("cy")(fakeRequest)
        cookies(result).get("PLAY_LANG").map(_.value) shouldBe Some("cy")
        redirectLocation(result) shouldBe Some(routes.HelloWorldController.helloWorld.url)
      }
    }

    "an invalid or unsupported language is supplied" should {
      "not change lang & redirect to referring URL when it is defined" in {
        val result = controller.switchToLanguage("foo")(fakeRequestWithReferer)
        cookies(result).get("PLAY_LANG").map(_.value).getOrElse("noLangFound") shouldBe "test"
        redirectLocation(result) shouldBe Some("/")
      }

      "not change lang & redirect to fallback URL when no referring URL is defined" in {
        val result = controller.switchToLanguage("foo")(fakeRequest)
        cookies(result).get("PLAY_LANG").map(_.value).getOrElse("noLangFound") shouldBe "test"
        redirectLocation(result) shouldBe Some(routes.HelloWorldController.helloWorld.url)
      }
    }
  }
}
