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

package handlers

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.MessagesApi
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.twirl.api.Html
import views.html.SomethingWentWrongView
import views.html.templates.ErrorTemplate

class ErrorHandlerSpec extends AnyWordSpec
  with Matchers
  with GuiceOneAppPerSuite
  with ScalaFutures:

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .build()

  private val fakeRequest = FakeRequest("GET", "/")
  private val handler     = app.injector.instanceOf[ErrorHandler]
  private val messages = app.injector.instanceOf[MessagesApi].preferred(fakeRequest)
  
  "internalServerErrorTemplate" should{
    "render the expected view" in {
      implicit val request: RequestHeader = fakeRequest
      val view: SomethingWentWrongView = app.injector.instanceOf[SomethingWentWrongView]
      val result: Html = await(handler.internalServerErrorTemplate)
      result shouldBe view()(fakeRequest, messages)
      result.contentType shouldBe "text/html"
    } 
  }

  "standardErrorTemplate" should:
    "render the expected view" in:
      implicit val request: RequestHeader = fakeRequest
      val view: ErrorTemplate = app.injector.instanceOf[ErrorTemplate]
      val result: Html = await(handler.standardErrorTemplate("title", "heading", "message"))
      result shouldBe view("title", "heading", "message")(request, messages)
      result.contentType shouldBe "text/html"
