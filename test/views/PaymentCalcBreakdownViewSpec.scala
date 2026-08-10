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
import models.userAnswers.LeppItemStatus.{Available, Paid}
import models.userAnswers.{LeppItem, LeppSummary}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.MessageKeys
import views.html.PaymentCalcBreakdownView

class PaymentCalcBreakdownViewSpec extends SpecBase {
  val leppItem: LeppItem = LeppItem(
    id = "id-1",
    taxYear = 2025,
    contributions = 1000,
    taxRate = 0.2,
    entitlement = 200,
    status = Available,
    claimDate = None,
    originalAmount = Some(100)
  )

  val summary: LeppSummary = LeppSummary(
    1, 
    Some(Seq(leppItem)), 
    Some(Seq(leppItem.copy(id = "P-id-1", taxYear = 2024, status = Paid)))
  )
  
  val multipleSummary = LeppSummary(
    1,
    Some(Seq(leppItem, leppItem.copy(id = "id-2")))
  )
  
  "view" - {
    "with correct LEPP gov banner" in new Setup {
      view.getElementsByClass("govuk-service-navigation__service-name").text() mustBe messages(app)("service.name")
      view.getElementsByClass("govuk-link hmrc-sign-out-nav__link").attr("href") mustBe
        "/accept-your-low-earners-pension-payment/account/sign-out-survey"
    }

    "display correct guidance and text for a single payment" in new Setup(summary, MessageKeys.SINGLE_UNDER_PAYMENT) {
      view.getElementsByTag("h1").text() mustBe messages(app)("breakdown.single.underpayment.heading", summary.totalEntitlementString)

      view.html.contains(messages(app)("breakdown.single.underpayment.p1"))
      view.text.contains(messages(app)("breakdown.u1"))
      view.text.contains(messages(app)("breakdown.l1"))
      view.text.contains(messages(app)("breakdown.l2"))
      view.text.contains(messages(app)("breakdown.l3"))
    }

    "display correct guidance and text for a multiple payment" in new Setup(multipleSummary, MessageKeys.MULTIPLE_PAYMENT, "£400") {
      view.getElementsByTag("h1").text() mustBe messages(app)("breakdown.multiple.heading", multipleSummary.totalEntitlementString)

      view.html.contains(messages(app)("breakdown.multiple.p1"))
    }

    "display continue link when not locked out" in new Setup {
      view.getElementsByClass("govuk-button govuk-button--continue").text() mustBe
        messages(app)("site.continue")
    }

    "display back to dashboard link when locked out" in new Setup(summary, MessageKeys.SINGLE_PAYMENT, "£200", true) {
      view.getElementById("barsLockFlag").text() mustBe messages(app)("bars.lockout.go-to-dashboard")
    }

    "display specific history item details" in new Setup(summary, MessageKeys.SINGLE_PAST_UNDER_PAYMENT, "£200", false, Some("P-id-1")) {
      view.getElementsByClass("govuk-summary-card__title-wrapper").text() mustBe "For the tax year 6 April 2024 to 5 April 2025"

      val elements: Elements = view.getElementById("P-id-1").getElementsByClass("govuk-summary-card__content")
      elements.forEach(
        element =>
          element.getElementsByClass("govuk-summary-list__key").first().text() mustBe "Your net pay pension contributions"
          element.getElementsByClass("govuk-summary-list__value").first().text() mustBe "£1000"

          element.getElementsByClass("govuk-summary-list__key").last().text() mustBe "Additional amount due"
          element.getElementsByClass("govuk-summary-list__value").last().text() mustBe "£200"
      )
    }

    "display available items details" in new Setup(summary, MessageKeys.SINGLE_UNDER_PAYMENT, "£200", false) {
      view.getElementById("id-1").getElementsByClass("govuk-summary-card__title").text() mustBe "For the tax year 6 April 2025 to 5 April 2026"
      
      val elements: Elements = view.getElementById("id-1").getElementsByClass("govuk-summary-card__content")
      elements.forEach(
        element =>
         element.getElementsByClass("govuk-summary-list__key").first().text() mustBe "Your net pay pension contributions"
         element.getElementsByClass("govuk-summary-list__value").first().text() mustBe "£1000"

         element.getElementsByClass("govuk-summary-list__key").last().text() mustBe "Additional amount due"
         element.getElementsByClass("govuk-summary-list__value").last().text() mustBe "£200"
      )
    }
  }

  trait Setup(summary: LeppSummary = summary,
              messageKey: String = MessageKeys.SINGLE_PAST_UNDER_PAYMENT,
              entitlement: String = "£200",
              barsLock: Boolean = false,
              id: Option[String] = None) {
    val app: Application = applicationBuilder(emptyUserAnswers).build()
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")
    
    val view: Document = Jsoup.parse(
      app.injector.instanceOf[PaymentCalcBreakdownView].apply(summary, "some-url", Some("back-url"), barsLock, id, messageKey, entitlement).body
    )
  }
}
