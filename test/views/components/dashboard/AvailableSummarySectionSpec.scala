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
import models.userAnswers.LeppItemStatus.Available
import models.userAnswers.{LeppItem, LeppSummary}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import views.html.components.dashboard.available_summary_section

class AvailableSummarySectionSpec extends SpecBase {

  "available_summary_section" - {
    "should produce expected HTML element if not locked" in new Setup() {
      val summaryView: Document = view(summaryModel, "/href", false, true)
      summaryView.html must include("""<strong class="govuk-!-font-weight-bold">£200</strong>""")
      summaryView.html must include("To accept this payment, you need to provide us with your bank details.")
      summaryView.html must include("""<a href="/href"""")
      summaryView.getElementsByClass("govuk-button govuk-button--continue").text() mustBe
        messages(app)("dashboard.availablePayments.button.acceptPayments")
    }

    "should produce expected HTML element when there are multiple available payments" in new Setup() {
      val availableItems: Seq[LeppItem] = summaryModel.availableItems.get :+
        LeppItem(
        id = "A-25-2",
        taxYear = 2025,
        contributions = 1000,
        taxRate = 20,
        entitlement = 200,
        status = Available,
        claimDate = None
      )
      val summaryView: Document = view(summaryModel.copy(availableItems = Some(availableItems)), "/href", false, true)
      summaryView.html must include("""<strong class="govuk-!-font-weight-bold">£400</strong>""")
      summaryView.html must include("To accept these payments, you need to provide us with your bank details.")
    }
    
    "should produce expected HTML element when there are no available payments" in new Setup() {
      val summaryView: Document = view(summaryModel, "/href", false, false)
      summaryView.html must include("""<strong class="govuk-!-font-weight-bold">£200</strong>""")
      summaryView.html mustNot include("To accept these payments, you need to provide us with your bank details.")
      summaryView.html mustNot include("""<a href="/href"""")
    }

    "display 'view payments' button when locked out" in new Setup() {
      val summaryView: Document = view(summaryModel, "/href", true, true)
      summaryView.getElementsByClass("govuk-button govuk-button--continue").text() mustBe
        messages(app)("dashboard.availablePayments.button.viewPayments")
    }
  }

  trait Setup() {
    val app: Application = applicationBuilder(emptyUserAnswers).build()
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")
    
    def view(leppSummary: LeppSummary, continueUrl: String, barsLockFlag: Boolean, hasAvailableItems: Boolean): Document = Jsoup.parse(
      app.injector.instanceOf[available_summary_section].apply(leppSummary, continueUrl, barsLockFlag, hasAvailableItems).body
    )
  }
}
