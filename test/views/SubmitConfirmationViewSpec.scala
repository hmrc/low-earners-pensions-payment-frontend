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
import models.userAnswers.LeppItem
import models.userAnswers.LeppItemStatus.Available
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalactic.Prettifier.default
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import views.html.SubmitConfirmationView

import java.time.Instant

class SubmitConfirmationViewSpec extends SpecBase {

  val acceptedItem = LeppItem(
    id = "A-25-1",
    taxYear = 2025,
    contributions = 1000,
    taxRate = 0.2,
    entitlement = 200,
    status = Available,
    claimDate = None
  )

  val acceptedItem2: LeppItem = acceptedItem.copy(id = "A-24-1", taxYear = 2024, entitlement = 201)

  val notAcceptedItem = LeppItem(
    id = "A-26-1",
    taxYear = 2026,
    contributions = 1000,
    taxRate = 0.2,
    entitlement = 300,
    status = Available,
    claimDate = None
  )

  val notAcceptedItem2: LeppItem = notAcceptedItem.copy(id = "A-27-1", taxYear = 2027, entitlement = 301)

  val successTableId = "confirmation_table_accepted_payments"
  val failureTableId = "confirmation_table_available_payments"

  "view" - {

    "should render the correct LEPP gov banner" in new Setup {
      view.getElementsByClass("govuk-service-navigation__service-name").text() mustBe messages(app)("service.name")
      view.getElementsByClass("govuk-link hmrc-sign-out-nav__link").attr("href") mustBe
        "/accept-your-low-earners-pension-payment/account/sign-out-survey"
    }

    "should display the correct content when" - {

      "there is one successful payment" in new Setup(Seq(acceptedItem)) {

        view.select("h1").text() mustBe "We've received your bank details"

        view.text.contains("We'll send this payment to the bank account you provided within 7 working days.")

        view.getElementById(s"${successTableId}_header_taxYear").text() mustBe "Tax year"
        view.getElementById(s"${successTableId}_header_taxYear").hasClass("govuk-table__header") mustBe true
        view.getElementById(s"${successTableId}_header_amount").text() mustBe "Amount"
        val taxYear: Int = acceptedItem.taxYear
        view.getElementById(s"taxYear_$taxYear").text() mustBe "6 April 2025 to 5 April 2026"
        view.getElementById(s"entitlement_$taxYear").text() mustBe "£200"

        view.text.contains("Submitted on")

        view.text.contains("What you can do next")
        view.text.contains("Print this page")
        view.text.contains("View your payments")
      }

      "when there are multiple successful payments" in new Setup(Seq(acceptedItem, acceptedItem2)) {

        view.select("h1").text() mustBe "We've received your bank details"

        view.text.contains("The following payments were processed successfully. " +
          "We'll send them separately to the bank account you provided within 7 working days.")

        view.getElementById(s"${successTableId}_header_taxYear").text() mustBe "Tax year"
        view.getElementById(s"${successTableId}_header_taxYear").hasClass("govuk-table__header") mustBe true
        view.getElementById(s"${successTableId}_header_amount").text() mustBe "Amount"
        val taxYear1: Int = acceptedItem.taxYear
        view.getElementById(s"taxYear_$taxYear1").text() mustBe "6 April 2025 to 5 April 2026"
        view.getElementById(s"entitlement_$taxYear1").text() mustBe "£200"
        val taxYear2: Int = acceptedItem2.taxYear
        view.getElementById(s"taxYear_$taxYear2").text() mustBe "6 April 2024 to 5 April 2025"
        view.getElementById(s"entitlement_$taxYear2").text() mustBe "£201"

        view.text.contains("Submitted on")

        view.text.contains("What you can do next")
        view.text.contains("Print this page")
        view.text.contains("View your payments")
      }

      "when there is one successful payment and one failed payment" in new Setup(Seq(acceptedItem),
                                                                                 Seq(notAcceptedItem)) {

        view.select("h1").text() mustBe "Some of your payments failed to process due to a system error"

        view.text.contains("Failed payments")
        view.getElementById(s"${failureTableId}_header_taxYear").text() mustBe "Tax year"
        view.getElementById(s"${failureTableId}_header_taxYear").hasClass("govuk-table__header") mustBe true
        view.getElementById(s"${failureTableId}_header_amount").text() mustBe "Amount"
        val failedTaxYear: Int = notAcceptedItem.taxYear
        view.getElementById(s"taxYear_$failedTaxYear").text() mustBe "6 April 2026 to 5 April 2027"
        view.getElementById(s"entitlement_$failedTaxYear").text() mustBe "£300"

        view.text.contains("To accept it again, view your payments and select 'Accept payment'.")

        view.text.contains("Successful payments")
        view.getElementById(s"${successTableId}_header_taxYear").text() mustBe "Tax year"
        view.getElementById(s"${successTableId}_header_taxYear").hasClass("govuk-table__header") mustBe true
        view.getElementById(s"${successTableId}_header_amount").text() mustBe "Amount"
        val successTaxYear: Int = acceptedItem.taxYear
        view.getElementById(s"taxYear_$successTaxYear").text() mustBe "6 April 2025 to 5 April 2026"
        view.getElementById(s"entitlement_$successTaxYear").text() mustBe "£200"

        view.text.contains("We'll send the payment to the bank account you provided within 7 working days.")

        view.text.contains("Submitted on")

        view.text.contains("What you can do next")
        view.text.contains("Print this page")
        view.text.contains("View your payments")
      }

      "when there are multiple successful and failed payments" in new Setup(Seq(acceptedItem, acceptedItem2),
                                                                            Seq(notAcceptedItem, notAcceptedItem2)) {

        view.select("h1").text() mustBe "Some of your payments failed to process due to a system error"

        view.text.contains("Failed payments")
        view.getElementById(s"${failureTableId}_header_taxYear").text() mustBe "Tax year"
        view.getElementById(s"${failureTableId}_header_taxYear").hasClass("govuk-table__header") mustBe true
        view.getElementById(s"${failureTableId}_header_amount").text() mustBe "Amount"
        val failedTaxYear1: Int = notAcceptedItem.taxYear
        view.getElementById(s"taxYear_$failedTaxYear1").text() mustBe "6 April 2026 to 5 April 2027"
        view.getElementById(s"entitlement_$failedTaxYear1").text() mustBe "£300"
        val failedTaxYear2: Int = notAcceptedItem2.taxYear
        view.getElementById(s"taxYear_$failedTaxYear2").text() mustBe "6 April 2027 to 5 April 2028"
        view.getElementById(s"entitlement_$failedTaxYear2").text() mustBe "£301"

        view.text.contains("To accept them again, view your payments and select 'Accept payments'.")

        view.text.contains("Successful payments")
        view.getElementById(s"${successTableId}_header_taxYear").text() mustBe "Tax year"
        view.getElementById(s"${successTableId}_header_taxYear").hasClass("govuk-table__header") mustBe true
        view.getElementById(s"${successTableId}_header_amount").text() mustBe "Amount"
        val successTaxYear1: Int = acceptedItem.taxYear
        view.getElementById(s"taxYear_$successTaxYear1").text() mustBe "6 April 2025 to 5 April 2026"
        view.getElementById(s"entitlement_$successTaxYear1").text() mustBe "£200"
        val successTaxYear2: Int = acceptedItem2.taxYear
        view.getElementById(s"taxYear_$successTaxYear2").text() mustBe "6 April 2024 to 5 April 2025"
        view.getElementById(s"entitlement_$successTaxYear2").text() mustBe "£201"

        view.text.contains("We'll send the payments separately to the bank account you provided within 7 working days.")

        view.text.contains("Submitted on")

        view.text.contains("What you can do next")
        view.text.contains("Print this page")
        view.text.contains("View your payments")
      }
    }
  }

  trait Setup(acceptedItems: Seq[LeppItem] = Nil, notAcceptedItems: Seq[LeppItem] = Nil) {
    val app: Application = applicationBuilder(emptyUserAnswers).build()
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")
    
    val view: Document = Jsoup.parse(
      app.injector.instanceOf[SubmitConfirmationView].apply(acceptedItems, notAcceptedItems, Instant.parse("2026-07-22T18:35:24.00Z")).body
    )
  }

}
