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

package views.auth

import base.SpecBase
import config.AppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import views.html.auth.WrongAccountUnauthorisedView

class WrongAccountUnauthorisedViewSpec extends SpecBase {

  "WrongAccountUnauthorisedView should" - {

    "render the correct content and link" in new Setup() {
      view.title() mustBe "Sign in with your personal tax account - Accept your low earner's pension payment - GOV.UK"
      view.select("h1").text() mustBe "Sign in with your personal tax account"
      view.select(".govuk-body").text() mustBe "You can only use this service with a personal tax account."
      view.select(".govuk-button").text() mustBe "Sign in"
      view.select(".govuk-button").attr("href") mustBe appConfig.loginWithContinueUrl
    }
  }

  trait Setup {
    val app: Application = applicationBuilder(emptyUserAnswers).build()
    val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

    val view: Document = Jsoup.parse(app.injector.instanceOf[WrongAccountUnauthorisedView].apply().body)
  }
}
