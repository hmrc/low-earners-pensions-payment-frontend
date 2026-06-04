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

package views

import base.SpecBase
import models.userAnswers.LeppItemStatus.Available
import models.userAnswers.{LeppItem, LeppSummary}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import views.html.PaymentCalcBreakdownView

class PaymentCalcBreakdownViewSpec extends SpecBase {

  val leppItem: LeppItem = LeppItem(
    id = "id-1",
    taxYear = 2025,
    contributions = 1000,
    taxRate = 20,
    entitlement = 200,
    status = Available,
    claimDate = None
  )
  val summary = LeppSummary(1, Some(Seq(leppItem)))
  
  "view" - {

    "with correct LEPP gov banner" in new Setup {
      view.getElementsByClass("govuk-service-navigation__service-name").text() mustBe messages(app)("service.name")
      view.getElementsByClass("govuk-link hmrc-sign-out-nav__link").attr("href") mustBe
        "/low-earners-pensions-payment/account/sign-out-survey"
    }

    "display correct guidance and text" in new Setup {
      view.getElementsByTag("h1").text() mustBe messages(app)("breakdown.heading", summary.totalEntitlementString)

      view.html.contains(messages(app)("breakdown.p1"))
      view.text.contains(messages(app)("breakdown.u1"))
      view.text.contains(messages(app)("breakdown.l1"))
      view.text.contains(messages(app)("breakdown.l2"))
      view.text.contains(messages(app)("breakdown.l3"))
    }

    "display continue link when not locked out" in new Setup {
      view.getElementsByClass("govuk-button govuk-button--continue").text() mustBe
        messages(app)("site.continue")
    }

    "display back to dashboard link when locked out" in new Setup(true) {
      
      view.getElementById("barsLockFlag").text() mustBe messages(app)("bars.lockout.go-to-dashboard")
    }
  }

  trait Setup(barsLock: Boolean = false) {

    val app: Application = applicationBuilder(emptyUserAnswers).build()
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")
    
    val view: Document = Jsoup.parse(
      app.injector.instanceOf[PaymentCalcBreakdownView].apply(summary, "some-url", barsLock).body
    )
  }
}
