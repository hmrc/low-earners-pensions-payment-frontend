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

package views.bars

import base.SpecBase
import controllers.routes
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import viewmodels.NormalMode
import viewmodels.formPages.FormPageViewModel
import views.html.bars.BarsRequestErrorsView

class BarsRequestErrorsViewSpec extends SpecBase {

  "BarsRequestErrorsView" - {
    "display correct LEPP gov banner" in new Setup {
      view.getElementsByClass("govuk-service-navigation__service-name").text() mustBe messages(app)("service.name")
      view.getElementsByClass("govuk-link hmrc-sign-out-nav__link").attr("href") mustBe
        "/accept-your-low-earners-pension-payment/account/sign-out-survey"
    }

    "should display correct page title" in new Setup {
      view.title() must include("We could not verify your bank account details - Accept your low earner's pension payment")
    }

    "display correct content" in new Setup {
      view.getElementsByTag("h1").text() mustBe messages(app)("barsRequestErrors.heading")

      view.html.contains(messages(app)("barsRequestErrors.p1"))
      view.html.contains(messages(app)("barsRequestErrors.u1.l1"))
      view.html.contains(messages(app)("barsRequestErrors.u1.l2"))
      view.html.contains(messages(app)("barsRequestErrors.u1.l3"))
      view.html.contains(messages(app)("barsRequestErrors.u1.l4"))
      view.text.contains(messages(app)("barsRequestErrors.p2"))
    }
  }

  trait Setup {
    val app: Application = applicationBuilder(emptyUserAnswers).build()
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")

    private val onSubmit = routes.WhatAreYourBankDetailsController.onSubmit(NormalMode)
    private val backLinkUrl = routes.WhatAreYourBankDetailsController.onSubmit(NormalMode).url
    private val viewModel: FormPageViewModel = FormPageViewModel(onSubmit = onSubmit, backLinkUrl = Some(backLinkUrl))
    
    val view: Document = Jsoup.parse(
      app.injector.instanceOf[BarsRequestErrorsView].apply(viewModel).body
    )
  }

}
