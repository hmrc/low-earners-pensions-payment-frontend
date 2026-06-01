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

package views.components.dashboard

import base.SpecBase
import models.*
import models.userAnswers.LeppItemStatus.*
import models.userAnswers.{LeppItem, LeppItemStatus, LeppSummary}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import views.html.components.dashboard.payment_history_section

import java.time.LocalDate

class PaymentHistorySectionSpec extends SpecBase {

  "payment_history_section" - {
    "should display correct content when no historical items exist" in new Setup {
      val model: LeppSummary = LeppSummary(currentLock = 1234)
      val resultView: Document = view(model)
      resultView.getElementsByClass("govuk-heading-m").text() must include("Payment history")
      resultView.html() must include("You do not have any previous payments.")
    }

    "should display correct content when cancelled items exist" in new Setup {
      val model: LeppSummary = LeppSummary(
        currentLock = 1234,
        cancelledItems = Some(Seq(
          LeppItem(
            id = "C-25-1",
            taxYear = 2025,
            contributions = 1000,
            taxRate = 20,
            entitlement = 200,
            status = Cancelled,
            claimDate = None
          )
        ))
      )
      val resultView: Document = view(model)
      resultView.getElementsByClass("govuk-table__caption").text() must include("Payment history")
     
      val cancelledInset: Elements = resultView.getElementsByClass("govuk-inset-text")
      cancelledInset.text() must include("We cancelled 1 of your payments. For more information, contact us (opens in new tab).")
      cancelledInset.html() must include("<strong class=\"govuk-!-font-weight-bold\">1</strong>")
      cancelledInset.html() must include("<a class=\"govuk-link govuk-link--no-visited-state\" href=\"/\">contact us (opens in new tab).</a>")
      
      resultView.getElementsByClass("govuk-table__header").text() mustBe "Tax year Amount Date accepted Status Action"
      resultView.getElementsByClass("govuk-table__cell").text() mustBe "6 April 2025 to 5 April 2026 £200 N/A Cancelled Check calculation"
      
      val tableCells: String = resultView.getElementsByClass("govuk-table__cell").html()
      tableCells must include("<strong class=\"govuk-tag govuk-tag--red\"> Cancelled </strong>")
      val linkUrl: String = "/low-earners-pensions-payment/breakdown?id=C-25-1"
      tableCells must include(s"<a class=\"govuk-link govuk-link--no-visited-state\" href=\"$linkUrl\">Check calculation</a>")
    }
    
    "should display correct content when paid items exist" in new Setup {
      val model: LeppSummary = LeppSummary(
        currentLock = 1234,
        paidItems = Some(Seq(
          LeppItem(
            id = "P-25-1",
            taxYear = 2025,
            contributions = 1000,
            taxRate = 20,
            entitlement = 200,
            status = Paid,
            claimDate = Some(LocalDate.of(2025, 7, 7))
          )
        ))
      )
      val resultView: Document = view(model)
      resultView.getElementsByClass("govuk-table__caption").text() must include("Payment history")
      
      val paidInsetContent: String = "Payments with the 'Paid' status will be in the bank account you provided within 7 working days."
      resultView.getElementsByClass("govuk-inset-text").text() must include(paidInsetContent)
      resultView.getElementsByClass("govuk-table__header").text() mustBe "Tax year Amount Date accepted Status Action"
      resultView.getElementsByClass("govuk-table__cell").text() mustBe "6 April 2025 to 5 April 2026 £200 7 July 2025 Paid Check calculation"

      val tableCells: String = resultView.getElementsByClass("govuk-table__cell").html()
      tableCells must include("<strong class=\"govuk-tag govuk-tag--green\"> Paid </strong>")
      val linkUrl: String = "/low-earners-pensions-payment/breakdown?id=P-25-1"
      tableCells must include(s"<a class=\"govuk-link govuk-link--no-visited-state\" href=\"$linkUrl\">Check calculation</a>")
    }
  }

  trait Setup {
    val app: Application = applicationBuilder(emptyUserAnswers).build()
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")

    def view(model: LeppSummary): Document = Jsoup.parse(
      app.injector.instanceOf[payment_history_section].apply(model, "table-ref").body
    )
  }
}
