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

package controllers

import base.SpecBase
import controllers.actions.FakeBarsLockoutAction
import forms.WhatAreYourBankDetailsFormProvider
import models.userAnswers.{BankAccountDetails, LeppSummary, UserAnswers}
import pages.{SubmissionPage, DashboardPage, WhatAreYourBankDetailsPage}
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.formPages.FormPageViewModel
import viewmodels.{CheckMode, NormalMode}
import views.html.WhatAreYourBankDetailsView

class WhatAreYourBankDetailsControllerSpec extends SpecBase {

  private lazy val onPageLoad = routes.WhatAreYourBankDetailsController.onPageLoad(NormalMode).url
  private lazy val onSubmit = routes.WhatAreYourBankDetailsController.onSubmit(NormalMode)
  private lazy val backLinkUrl = routes.PaymentCalcBreakdownController.onPageLoad(None).url
  private val formProvider = new WhatAreYourBankDetailsFormProvider()
  private val form: Form[BankAccountDetails] = formProvider()

  val accountName = "Tax Payer"
  val accountNumber = "12345678"
  val sortCode = "112233"
  val rollNumber = "1234678"

  private val bankAccountDetails: BankAccountDetails = BankAccountDetails(
    accountName = accountName,
    accountNumber = accountNumber,
    sortCode = sortCode,
    rollNumber = Some(rollNumber)
  )

  val userAnswers: UserAnswers =
    emptyUserAnswers.set(page = DashboardPage, value = summaryModel).success.value
      .set(page = WhatAreYourBankDetailsPage, value = bankAccountDetails).success.value

  val summaryUserAnswers: UserAnswers =
    emptyUserAnswers.set(page = DashboardPage, value = summaryModel).success.value

  "WhatAreYourBankDetailsController" - {
    
    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = summaryUserAnswers).build()

      running(application) {
        val request = FakeRequest(GET, onPageLoad)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WhatAreYourBankDetailsView]
        val viewModel: FormPageViewModel = getFormPageViewModel(onSubmit, backLinkUrl)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, viewModel)(request, messages(application)).toString
      }
    }

    "must return OK and pre-fill the form when data is already present" in {

      val application = applicationBuilder(userAnswers).build()

      running(application) {
        val request = FakeRequest(GET, onPageLoad)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WhatAreYourBankDetailsView]
        val viewModel: FormPageViewModel = getFormPageViewModel(onSubmit, backLinkUrl)
        val expectedForm: Form[BankAccountDetails] = form.fill(bankAccountDetails)
        val expectedViewString: String =
          view(expectedForm, viewModel)(request, messages(application)).toString

        status(result) mustEqual OK
        contentAsString(result) mustEqual expectedViewString
      }
    }

    "must return OK and pre-fill the form in the edit mode" in {

      val application = applicationBuilder(userAnswers).build()

      val onPageLoadEdit = routes.WhatAreYourBankDetailsController.onPageLoad(CheckMode).url
      val onSubmitEdit = routes.WhatAreYourBankDetailsController.onSubmit(CheckMode)
      val editBackLinkUrl = routes.CheckYourAnswersController.onPageLoad().url
      
      running(application) {
        val request = FakeRequest(GET, onPageLoadEdit)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WhatAreYourBankDetailsView]
        val viewModel: FormPageViewModel = getFormPageViewModel(onSubmitEdit, editBackLinkUrl)
        val expectedForm: Form[BankAccountDetails] = form.fill(bankAccountDetails)
        val expectedViewString: String =
          view(expectedForm, viewModel)(request, messages(application)).toString

        status(result) mustEqual OK
        contentAsString(result) mustEqual expectedViewString
      }
    }

    "must redirect to Dashboard page when no payment data exists" in {

      val application = applicationBuilder(emptyUserAnswers).build()

      running(application) {
        val request = FakeRequest(GET, onPageLoad)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.DashboardController.onPageLoad().url
      }
    }

    "must redirect to clear cache controller when already submitting the request" in {

      val userAnswers = emptyUserAnswers.set(page = SubmissionPage, value = true).success.value
      val application = applicationBuilder(userAnswers).build()

      running(application) {
        val request = FakeRequest(GET, onPageLoad)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ClearCacheController.onPageLoad().url
      }
    }

    "must redirect to BARS lockout controller when user made too many bars check attempts" in {
      
      val application = applicationBuilder(userAnswers, barsLockoutAction = FakeBarsLockoutAction(3)).build()

      running(application) {
        val request = FakeRequest(GET, onPageLoad)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual bars.routes.BarsLockoutController.onPageLoad().url
      }
    }
  }
}
