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
import controllers.routes
import models.*
import models.userAnswers.BankAccountDetails
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import pages.TempPage.Confirmation
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.NormalMode
import viewmodels.checkYourAnswers.CheckYourAnswersSummary.*
import viewmodels.formPages.FormPageViewModel
import views.html.CheckYourAnswersView

import java.time.LocalDate

class CheckYourAnswersViewSpec extends SpecBase {

  "view" - {
    "display correct guidance and text" in new Setup {
      view.getElementsByTag("h1").text() mustBe msg("Check your answers")
      view.getElementsByTag("h2").text() must include(msg("Your bank details"))

      view.html must include(msg("checkYourAnswers.title"))
      view.html must include(msg("bankDetails.accountName"))
      view.html must include(msg("bankDetails.accountNumber"))
      view.html must include(msg("bankDetails.sortCode"))
      view.html must include(msg("bankDetails.rollNumber"))
    }

    "not display roll number row when user has not submitted a value" in new Setup {
      override val accountDetails: BankAccountDetails = BankAccountDetails(
        accountName = "Some Name",
        accountNumber = "12345678",
        sortCode = "11-22-33",
        rollNumber = None
      )

      view.getElementsByTag("h1").text() mustBe msg("Check your answers")
      view.getElementsByTag("h2").text() must include(msg("Your bank details"))

      view.html must include(msg("checkYourAnswers.title"))
      view.html must include(msg("bankDetails.accountName"))
      view.html must include(msg("bankDetails.accountNumber"))
      view.html must include(msg("bankDetails.sortCode"))
      view.html must not include msg("bankDetails.rollNumber")
    }
  }

  trait Setup {
    val app: Application = applicationBuilder(emptyUserAnswers).build()
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")

    val accountDetails: BankAccountDetails = BankAccountDetails(
      accountName = "Some Name",
      accountNumber = "12345678",
      sortCode = "11-22-33",
      rollNumber = Some("12345/678")
    )

    lazy val summaryList: Seq[SummaryListRow] = cyaSummaryList(accountDetails)

    val backLinkUrl: String = routes.WhatAreYourBankDetailsController.onSubmit(NormalMode).url

    val viewModel: FormPageViewModel = FormPageViewModel(
      onSubmit = routes.TempLeppController.onPageLoad(Confirmation),
      backLinkUrl = Some(routes.WhatAreYourBankDetailsController.onPageLoad(NormalMode).url)
    )

    lazy val view: Document = Jsoup.parse(
      app.injector.instanceOf[CheckYourAnswersView].apply(summaryList, viewModel).body
    )
  }
}
