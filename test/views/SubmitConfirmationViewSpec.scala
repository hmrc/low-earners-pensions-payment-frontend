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
import utils.CurrencyFormats
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
  
  val acceptedItems: Seq[LeppItem] = Seq(acceptedItem)
  
  val notAcceptedItems: Seq[LeppItem] = Seq(
    LeppItem(
      id = "A-26-1",
      taxYear = 2026,
      contributions = 1000,
      taxRate = 0.2,
      entitlement = 200,
      status = Available,
      claimDate = None
    )
  )
  
  "view" - {

    "with correct LEPP gov banner" in new Setup {
      view.getElementsByClass("govuk-service-navigation__service-name").text() mustBe messages(app)("service.name")
      view.getElementsByClass("govuk-link hmrc-sign-out-nav__link").attr("href") mustBe
        "/accept-your-low-earners-pension-payment/account/sign-out-survey"
    }

    "display correct guidance and text" in new Setup(acceptedItems = acceptedItems) {
      view.getElementsByTag("h1").text() mustBe messages(app)("confirmation.heading")

      view.text.contains(messages(app)("confirmation.details"))
      view.text.contains(messages(app)("confirmation.what-next.opt1"))
      view.text.contains(messages(app)("confirmation.what-next.opt2.sec1"))
      view.text.contains(messages(app)("confirmation.what-next.opt2.sec2"))

      view.text.contains(messages(app)("submitted.on"))
    }

    "display correct accepted payments" in new Setup(
      acceptedItems = acceptedItems :+ acceptedItem.copy(id = "A-25-2"),
      notAcceptedItems = notAcceptedItems
    ) {
      
      val taxYear: Int = summaryModel.availableItems.get.head.taxYear
      val entitlement: BigDecimal = summaryModel.availableItems.get.head.entitlement
      view.text.contains(messages(app)("confirmation.multiple.details"))
      view.getElementById("confirmation_table_accepted_payments_header_taxYear").text() mustBe messages(app)("confirmation.table.header.taxYear")
      view.getElementById("confirmation_table_accepted_payments_header_taxYear").hasClass("govuk-table__header") mustBe true
      view.getElementById("confirmation_table_accepted_payments_header_amount").text() mustBe messages(app)("confirmation.table.header.amount")
      view.getElementById(s"taxYear_$taxYear").text() mustBe messages(app)("common.taxYearDates", s"$taxYear", s"${taxYear + 1}")
      view.getElementById(s"entitlement_$taxYear").text() mustBe CurrencyFormats.format(entitlement)
      
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
