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
import models.userAnswers.LeppSummary
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import views.html.components.dashboard.payment_history_section

class PaymentHistorySectionSpec extends SpecBase {

  "payment_history_section should" - {

    "render inset text elements when there are paid and cancelled payments" in new Setup() {
      val result: Document = view(summaryModel, tableRef)

      result.select("h2").text() mustBe "Payment history"
      result.select(".govuk-inset-text:nth-of-type(1)").text() mustBe
        "We cancelled 1 of your payments. Cancelled payments will be replaced by a new payment."
      result.select(".govuk-inset-text:nth-of-type(2)").text() mustBe
        "Payments with the Paid status will be in the bank account you provided within 7 working days."
    }

    "render no inset text, and a separate information paragraph if there is no payment history" in new Setup() {
      val summary: LeppSummary = summaryModel.copy(paidItems = None, cancelledItems = None)
      val result: Document = view(summary, tableRef)

      result.select("h2").text() mustBe "Payment history"
      result.select(".govuk-inset-text").size() mustBe 0
      result.select(".govuk-body").text() mustBe "You do not have any previous payments."
    }

    "should produce the expected table contents" in new Setup() {
      val result: Document = view(summaryModel, tableRef)
      result.getElementById(s"${tableRef}_header_taxYear").text() mustBe "Tax year"
      result.getElementById(s"${tableRef}_header_amount").text() mustBe "Amount"
      result.getElementById(s"${tableRef}_header_dateAccepted").text() mustBe "Date accepted"
      result.getElementById(s"${tableRef}_header_status").text() mustBe "Status"
      result.getElementById(s"${tableRef}_header_action").text() mustBe "Action"

      result.select("tbody tr").size() mustBe summaryModel.paymentHistoryItems.size

      val row1 = "tbody > tr:nth-of-type(1)"
      result.select(s"$row1 > th").text() mustBe "6 April 2025 to 5 April 2026"
      result.select(s"$row1 > td:nth-of-type(1)").text() mustBe "£200"
      result.select(s"$row1 > td:nth-of-type(2)").text() mustBe "N/A"
      result.select(s"$row1 > td:nth-of-type(3)").text() mustBe "Cancelled"
      result.select(s"$row1 > td:nth-of-type(4)").text() mustBe "Check calculation 6 April 2025 to 5 April 2026"
      result.select(s"$row1 > td:nth-of-type(4) .govuk-visually-hidden").text() mustBe "6 April 2025 to 5 April 2026"

      val row2 = "tbody > tr:nth-of-type(2)"
      result.select(s"$row2 > th").text() mustBe "6 April 2025 to 5 April 2026"
      result.select(s"$row2 > td:nth-of-type(1)").text() mustBe "£200"
      result.select(s"$row2 > td:nth-of-type(2)").text() mustBe "N/A"
      result.select(s"$row2 > td:nth-of-type(3)").text() mustBe "Paid"
      result.select(s"$row2 > td:nth-of-type(4)").text() mustBe "Check calculation 6 April 2025 to 5 April 2026"
      result.select(s"$row2 > td:nth-of-type(4) .govuk-visually-hidden").text() mustBe "6 April 2025 to 5 April 2026"
    }
  }

  trait Setup() {
    val app: Application = applicationBuilder(emptyUserAnswers).build()
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

    val tableRef = "tableRef"

    def view(leppSummary: LeppSummary, tableRef: String): Document = Jsoup.parse(
      app.injector.instanceOf[payment_history_section].apply(leppSummary, tableRef).body
    )
  }
}
