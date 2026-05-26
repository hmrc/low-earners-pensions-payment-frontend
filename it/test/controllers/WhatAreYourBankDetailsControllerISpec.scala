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

import common.IntegrationSpecBase
import forms.WhatAreYourBankDetailsFormProvider
import models.userAnswers.*
import play.api.Application
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.SessionKeys
import viewmodels.formPages.FormPageViewModel
import viewmodels.{CheckMode, NormalMode}
import views.html.WhatAreYourBankDetailsView

import scala.concurrent.Future

class WhatAreYourBankDetailsControllerISpec extends ControllerIntegrationSpecBase {
  private val backUrl: String = routes.PaymentCalcBreakdownController.onPageLoad().url
  private val onSubmitUrl: Call = routes.WhatAreYourBankDetailsController.onSubmit(NormalMode)

  private val formViewModel: FormPageViewModel = FormPageViewModel(
    onSubmit = onSubmitUrl,
    backLinkUrl = Some(backUrl)
  )

  private val formProvider: WhatAreYourBankDetailsFormProvider = new WhatAreYourBankDetailsFormProvider
  private val form: Form[BankAccountDetails] = formProvider()

  "GET /bank-details" when {
    val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
      method = "GET",
      path = "/low-earners-pensions-payment/bank-details"
    ).withSession(SessionKeys.authToken -> "auth token")
   
    testUserAnswersHandling(request = request)

    "existing user answers are found" should {
      "return view with filled answers" in {
        mockAuthSuccess()
        mockBarsLockoutAction(url = "/low-earners-pensions-payment/bars/verify/status", status = OK,
          response = Json.obj("attempts" -> 1))
        
        val application: Application = applicationWithUserAnswers(userAnswersWithBankDetails)

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        val view = application.injector.instanceOf[WhatAreYourBankDetailsView]
        val messages: Messages = application.injector.instanceOf[MessagesApi].preferred(request)
        val filledForm: Form[BankAccountDetails] = form.fill(bankAccountDetails)

        status(result) shouldBe OK
        contentAsString(result) shouldEqual view(filledForm, formViewModel)(request, messages).toString
      }
    }

    "existing user answers are not found" should {
      "return blank view" in {
        mockAuthSuccess()
        mockBarsLockoutAction(url = "/low-earners-pensions-payment/bars/verify/status", status = OK,
          response = Json.obj("attempts" -> 1))
        val application: Application = applicationWithUserAnswers(userAnswersWithLeppSummary)

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        val view: WhatAreYourBankDetailsView = application.injector.instanceOf[WhatAreYourBankDetailsView]
        val messages: Messages = application.injector.instanceOf[MessagesApi].preferred(request)

        status(result) shouldBe OK
        contentAsString(result) shouldEqual view(form, formViewModel)(request, messages).toString
      }
    }
  }

  "GET /change-bank-details" when {
    val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
      method = "GET",
      path = "/low-earners-pensions-payment/change-bank-details"
    ).withSession(SessionKeys.authToken -> "auth token")

    "page is loaded should include the correct back link" in {
      val backUrl: String = routes.CheckYourAnswersController.onPageLoad().url
      val onSubmitUrl: Call = routes.WhatAreYourBankDetailsController.onSubmit(CheckMode)

      mockAuthSuccess()
      mockBarsLockoutAction(url = "/low-earners-pensions-payment/bars/verify/status", status = OK,
        response = Json.obj("attempts" -> 1))
      val application: Application = applicationWithUserAnswers(userAnswersWithBankDetails)

      lazy val result: Future[Result] = route(application, request).getOrElse(
        Future.failed(new RuntimeException("TEST_ERROR"))
      )

      val view: WhatAreYourBankDetailsView = application.injector.instanceOf[WhatAreYourBankDetailsView]

      val formViewModel: FormPageViewModel = FormPageViewModel(
        mode = CheckMode,
        onSubmit = onSubmitUrl,
        backLinkUrl = Some(backUrl)
      )

      val filledForm: Form[BankAccountDetails] = form.fill(bankAccountDetails)
      val messages: Messages = application.injector.instanceOf[MessagesApi].preferred(request)

      status(result) shouldBe OK
      contentAsString(result) shouldEqual view(filledForm, formViewModel)(request, messages).toString
    }
  }

  "POST /bank-details" when {
    def request(data: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(
      method = "POST",
      path = "/low-earners-pensions-payment/bank-details"
    )
      .withSession(SessionKeys.authToken -> "auth token")
      .withFormUrlEncodedBody(data: _*)

    testUserAnswersHandling(request = request())

    "errors exist in supplied data" should {
      def testErrorScenario(scenarioName: String,
                            accountName: Option[String],
                            sortCode: Option[String],
                            accountNumber: Option[String],
                            rollNumber: Option[String]): Unit = s"handle correctly for scenario - $scenarioName" in {
        mockAuthSuccess()
        mockBarsLockoutAction(url = "/low-earners-pensions-payment/bars/verify/status", status = OK,
          response = Json.obj("attempts" -> 1))
        
        val app: Application = applicationWithUserAnswers(userAnswersWithLeppSummary)

        val formData: Seq[(String, String)] = Seq(
          accountName.map(name => "bankDetails.accountName" -> name),
          sortCode.map(sc => "bankDetails.sortCode" -> sc),
          accountNumber.map(an => "bankDetails.accountNumber" -> an),
          rollNumber.map(rn => "bankDetails.rollNumber" -> rn)
        ).flatten

        val formWithData: Form[BankAccountDetails] = form.bind(formData.toMap)
        val requestWithFormData: FakeRequest[AnyContentAsFormUrlEncoded] = request(formData: _*)

        val result: Future[Result] = route(app, request(formData: _*)).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        val view: WhatAreYourBankDetailsView = app.injector.instanceOf[WhatAreYourBankDetailsView]
        val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(requestWithFormData)

        formWithData.errors should not be empty
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) shouldEqual view(formWithData, formViewModel)(requestWithFormData, messages).toString
      }

      Seq(
        ("accountName field is missing", None, Some("11-22-33"), Some("12345678"), None),
        ("accountName field is empty", Some(""), Some("11-22-33"), Some("12345678"), None),
        ("accountName field has only whitespace", Some("       "), Some("11-22-33"), Some("12345678"), None),
        ("accountName field is invalid", Some("!!!!!"), Some("11-22-33"), Some("12345678"), None),
        ("accountName field is too long", Some("A" * 20), Some("11-22-33"), Some("12345678"), None),
        ("sortCode field is missing", Some("Name"), None, Some("12345678"), None),
        ("sortCode field is empty", Some("Name"), Some(""), Some("12345678"), None),
        ("sortCode field has only whitespace", Some("Name"), Some("    "), Some("12345678"), None),
        ("sortCode field is too short", Some("Name"), Some("11-22-3"), Some("12345678"), None),
        ("sortCode field is too long", Some("Name"), Some("11-22-333"), Some("12345678"), None),
        ("sortCode field is invalid", Some("Name"), Some("AAAAAA"), Some("12345678"), None),
        ("accountNumber field is missing", Some("Name"), Some("AAAAAA"), None, None),
        ("accountNumber field is empty", Some("Name"), Some("AAAAAA"), Some(""), None),
        ("accountNumber field has only whitespace", Some("Name"), Some("AAAAAA"), Some("     "), None),
        ("accountNumber field is invalid", Some("Name"), Some("AAAAAA"), Some("AAAAAA"), None),
        ("accountNumber field is too short", Some("Name"), Some("AAAAAA"), Some("12345"), None),
        ("accountNumber field is too long", Some("Name"), Some("AAAAAA"), Some("123456789"), None),
        ("rollNumber field is too long", Some("Name"), Some("11-22-33"), Some("12345678"), Some("a" * 20)),
        ("rollNumber field is invalid", Some("Name"), Some("11-22-33"), Some("12345678"), Some("!!!!!!!!")),
      ).foreach(testErrorScenario)
    }

    "a valid request is submitted" should {
      "redirect to the CYA page" in {
        mockAuthSuccess()
        mockBarsLockoutAction(url = "/low-earners-pensions-payment/bars/verify/status", status = OK,
          response = Json.obj("attempts" -> 1))
        
        val formData: Seq[(String, String)] = Seq(
          "bankDetails.accountName" -> "Taxwell Payer",
          "bankDetails.sortCode" -> "11-22-33",
          "bankDetails.accountNumber" -> "12345678",
          "bankDetails.rollNumber" -> "1234/678"
        )

        val app: Application = applicationWithUserAnswers(userAnswersWithLeppSummary)

        val result: Future[Result] = route(app, request(formData: _*)).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad().url)
      }
    }
  }

  "POST /change-bank-details" when {

    def request(data: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(
      method = "POST",
      path = "/low-earners-pensions-payment/change-bank-details"
    )
      .withSession(SessionKeys.authToken -> "auth token")
      .withFormUrlEncodedBody(data: _*)

    "errors exist in supplied data" should {
      "load page with errors and CYA back link" in {
        mockAuthSuccess()
        mockBarsLockoutAction(url = "/low-earners-pensions-payment/bars/verify/status", status = OK,
          response = Json.obj("attempts" -> 1))
        val app: Application = applicationWithUserAnswers(userAnswersWithLeppSummary)

        val formData: Seq[(String, String)] = Seq(
          "bankDetails.accountName" -> "Taxwell Payer",
          "bankDetails.accountNumber" -> "12345678",
          "bankDetails.rollNumber" -> "1234/678"
        )

        val formWithData: Form[BankAccountDetails] = form.bind(formData.toMap)
        val requestWithFormData: FakeRequest[AnyContentAsFormUrlEncoded] = request(formData: _*)

        val result: Future[Result] = route(app, request(formData: _*)).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        val backUrl: String = routes.CheckYourAnswersController.onPageLoad().url
        val onSubmitUrl: Call = routes.WhatAreYourBankDetailsController.onSubmit(CheckMode)

        val formViewModel: FormPageViewModel = FormPageViewModel(
          mode = CheckMode,
          onSubmit = onSubmitUrl,
          backLinkUrl = Some(backUrl)
        )

        val view: WhatAreYourBankDetailsView = app.injector.instanceOf[WhatAreYourBankDetailsView]
        val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(requestWithFormData)

        formWithData.errors should not be empty
        status(result) shouldBe BAD_REQUEST
        contentAsString(result) shouldEqual view(formWithData, formViewModel)(requestWithFormData, messages).toString
      }
    }

    "a valid request is submitted" should {
      "redirect to the CYA page" in {
        mockAuthSuccess()
        mockBarsLockoutAction(url = "/low-earners-pensions-payment/bars/verify/status", status = OK,
          response = Json.obj("attempts" -> 1))
        
        val formData: Seq[(String, String)] = Seq(
          "bankDetails.accountName" -> "Taxwell Payer",
          "bankDetails.sortCode" -> "11-22-33",
          "bankDetails.accountNumber" -> "12345678",
          "bankDetails.rollNumber" -> "1234/678"
        )

        val app: Application = applicationWithUserAnswers(userAnswersWithLeppSummary)

        val result: Future[Result] = route(app, request(formData: _*)).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad().url)
      }
    }
  }
}