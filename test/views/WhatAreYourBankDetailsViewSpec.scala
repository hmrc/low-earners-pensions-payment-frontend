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
import forms.WhatAreYourBankDetailsFormProvider
import models.userAnswers.BankAccountDetails
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import viewmodels.NormalMode
import viewmodels.formPages.FormPageViewModel
import views.html.WhatAreYourBankDetailsView

import scala.jdk.CollectionConverters.*

class WhatAreYourBankDetailsViewSpec extends SpecBase {
  
  private trait Test {
    val app: Application = applicationBuilder().build()
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")

    private val formProvider = new WhatAreYourBankDetailsFormProvider()
    private val form: Form[BankAccountDetails] = formProvider()
    private val onSubmit = routes.WhatAreYourBankDetailsController.onSubmit(NormalMode)
    private val backLinkUrl = routes.WhatAreYourBankDetailsController.onSubmit(NormalMode).url
    private val viewModel: FormPageViewModel = FormPageViewModel(onSubmit = onSubmit, backLinkUrl = Some(backLinkUrl))

    val view: Document =
      Jsoup.parse(app.injector.instanceOf[WhatAreYourBankDetailsView].apply(form, viewModel).body)
  }

  "WhatAreYourBankDetailsView" - {
    "should display correct page title" in new Test {
      view.title() must include("Bank details - Accept your low earner's pension payment")
    }
    
    "should render header and inset text correctly" in new Test {
      view.getElementsByTag("h1").text() mustBe messages(app)("What are your bank details?")
      view.getElementsByClass("govuk-inset-text").text() mustBe messages(app)(
        "For your security, we do not save your bank details."
      )
    }
    
    s"should render form group correctly" - new Test {
      val formGroups: Iterator[Element] = view.getElementsByClass("govuk-form-group").iterator().asScala

      val inputContents: Map[String, (String, String)] = (for {
        formGroup <- formGroups
        labelText = formGroup.getElementsByClass("govuk-label").text()
        hintText = formGroup.getElementsByClass("govuk-hint").text()
        inputName = formGroup.getElementsByClass("govuk-input").attr("name")
      } yield inputName -> (labelText, hintText)).toMap
      
      def forField(fieldName: String, label: String, hint: String): Unit = s"for field: $fieldName" in {
        val (labelText, hintText) = inputContents.getOrElse(fieldName, ("N/A", "N/A"))
        labelText mustBe label
        hintText mustBe hint
      }

      Seq(
        ("bankDetails.accountName", "Name on the account", "Exactly as it appears on your bank statement"),
        ("bankDetails.sortCode", "Sort code", "Must be 6 digits long"),
        ("bankDetails.accountNumber", "Account number", "Must be between 6 and 8 digits long"),
        ("bankDetails.rollNumber", "Building society roll number (if you have one)", "You can find it on your card, statement or passbook")
      ).foreach((fieldName, label, hint) => forField(fieldName, label, hint))
    }
    
    "should render continue button correctly" in new Test {
      view.getElementsByClass("govuk-button").text() mustBe messages(app)("Continue")
    }
  }
}
