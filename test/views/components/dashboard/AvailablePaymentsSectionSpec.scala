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
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import views.html.components.dashboard.available_payments_section

class AvailablePaymentsSectionSpec extends SpecBase {

  "available_payments_section" - {
    "should display correct content when no available items exist" in new Setup {
      val model: LeppSummary = LeppSummary(currentLock = 1234)
      val resultView: Document = view(model)
      resultView.getElementsByClass("govuk-heading-m").text() must include("Available payments")
      resultView.html() must include("You do not have any available payments.")
    }

    "should display correct content when suspended items exist" in new Setup {
      val model: LeppSummary = LeppSummary(
        currentLock = 1234,
        suspendedItems = Some(Seq(
          LeppItem(
            id = "S-25-1",
            taxYear = 2025,
            contributions = 1000,
            taxRate = 20,
            entitlement = 200,
            status = Suspended,
            claimDate = None
          )
        ))
      )
      val resultView: Document = view(model)
      resultView.getElementsByClass("govuk-table__caption").text() must include("Available payments")

      val suspendedInsetContent: String = "Your payments are suspended. For more information, contact us (opens in new tab)."
      val suspendedInset: Elements = resultView.getElementsByClass("govuk-inset-text")
      suspendedInset.text() must include(suspendedInsetContent)
      suspendedInset.html() must include("<a class=\"govuk-link govuk-link--no-visited-state\" href=\"/\">contact us (opens in new tab).</a>")

      resultView.getElementsByClass("govuk-table__header").text() mustBe "Tax year Amount Available until Status"
      resultView.getElementsByClass("govuk-table__cell").text() mustBe "6 April 2025 to 5 April 2026 £200 5 April 2030 Suspended"

      val tableCells: String = resultView.getElementsByClass("govuk-table__cell").html()
      tableCells must include("<strong class=\"govuk-tag govuk-tag--yellow\"> Suspended </strong>")
      
      val button: Element = resultView.getElementById("accept-payments-button")
      button mustBe null
    }
    
    "should display correct content when available items exist" in new Setup {
      val model: LeppSummary = LeppSummary(
        currentLock = 1234,
        availableItems = Some(Seq(
          LeppItem(
            id = "A-25-1",
            taxYear = 2025,
            contributions = 1000,
            taxRate = 20,
            entitlement = 200,
            status = Available,
            claimDate = None
          )
        ))
      )
      val resultView: Document = view(model)
      resultView.getElementsByClass("govuk-table__caption").text() must include("Available payments")
      
      resultView.getElementsByClass("govuk-table__header").text() mustBe "Tax year Amount Available until Status"
      resultView.getElementsByClass("govuk-table__cell").text() mustBe "6 April 2025 to 5 April 2026 £200 5 April 2030 Available"

      val tableCells: String = resultView.getElementsByClass("govuk-table__cell").html()
      tableCells must include("<strong class=\"govuk-tag govuk-tag--blue\"> Available </strong>")
      
      resultView.text() must include("You have a total of £200 in payments available to accept.")
      resultView.html() must include("<strong class=\"govuk-!-font-weight-bold\">£200</strong>")
      resultView.text() must include("To accept these payments, you need to provide us with your bank details.")
      
      val button: Element = resultView.getElementById("accept-payments-button")
      button.text() mustBe "Accept payments"
      button.attributes().toString must include("href=\"continue-url\"")
    }
  }

  trait Setup {
    val app: Application = applicationBuilder(emptyUserAnswers).build()
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")

    def view(model: LeppSummary): Document = Jsoup.parse(
      app.injector.instanceOf[available_payments_section].apply(model, "table-ref", "continue-url").body
    )
  }
}
