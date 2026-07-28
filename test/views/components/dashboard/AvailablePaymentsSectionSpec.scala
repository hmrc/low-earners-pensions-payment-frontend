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
import views.html.components.dashboard.available_payments_section

class AvailablePaymentsSectionSpec extends SpecBase {

  "available_payments_section" - {

    "should produce the expected table contents" in new Setup() {
      val result: Document = view(summaryModel, tableRef, "/href", false)
      result.getElementById(s"${tableRef}_header_taxYear").text() mustBe "Tax year"
      result.getElementById(s"${tableRef}_header_amount").text() mustBe "Amount"
      result.getElementById(s"${tableRef}_header_availableUntil").text() mustBe "Available until"
      result.getElementById(s"${tableRef}_header_status").text() mustBe "Status"

      result.select("tbody tr").size() mustBe summaryModel.availablePaymentItems.size

      result.select("tbody > tr:nth-of-type(1) > th").text() mustBe "6 April 2025 to 5 April 2026"
      result.select("tbody > tr:nth-of-type(1) > td:nth-of-type(1)").text() mustBe "£200"
      result.select("tbody > tr:nth-of-type(1) > td:nth-of-type(2)").text() mustBe "5 April 2030"
      result.select("tbody > tr:nth-of-type(1) > td:nth-of-type(3)").text() mustBe "Available"

      result.select("tbody > tr:nth-of-type(2) > th").text() mustBe "6 April 2025 to 5 April 2026"
      result.select("tbody > tr:nth-of-type(2) > td:nth-of-type(1)").text() mustBe "£200"
      result.select("tbody > tr:nth-of-type(2) > td:nth-of-type(2)").text() mustBe "5 April 2030"
      result.select("tbody > tr:nth-of-type(2) > td:nth-of-type(3)").text() mustBe "Suspended"
    }
  }

  trait Setup() {
    val app: Application = applicationBuilder(emptyUserAnswers).build()
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

    val tableRef = "tableRef"

    def view(leppSummary: LeppSummary, tableRef: String, continueUrl: String, barsLockFlag: Boolean): Document = Jsoup.parse(
      app.injector.instanceOf[available_payments_section].apply(leppSummary, tableRef, continueUrl, barsLockFlag).body
    )
  }
}
