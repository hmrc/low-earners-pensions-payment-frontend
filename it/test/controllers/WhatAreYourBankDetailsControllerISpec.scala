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
import models.userAnswers.LeppItemStatus.Available
import models.userAnswers.*
import play.api.Application
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.{JsValue, Json}
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
  
  private val userAnswersWithLepp: UserAnswers = UserAnswers(
    id = "1",
    data = Json.obj(
      "leppSummary" -> Json.toJson(summaryModel)
    )
  )

  "GET /bank-details" when {
    val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
      method = "GET",
      path = "/low-earners-pensions-payment/bank-details"
    ).withSession(SessionKeys.authToken -> "auth token")

    testControllerAuth(request)
    testSessionDataHandling(request)
    testLeppDataHandling(request)

    "existing user answers are found" should {
      "return view with filled answers" in {
        mockAuthSuccess()

        val application: Application = applicationWithUserAnswers(userAnswers)

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
        
        val application: Application = applicationWithUserAnswers(userAnswersWithLepp)

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

    "loaded should include the correct back link" in {
      val backUrl: String = routes.CheckYourAnswersController.onPageLoad().url
      val onSubmitUrl: Call = routes.WhatAreYourBankDetailsController.onSubmit(CheckMode)

      mockAuthSuccess()
      val application: Application = applicationWithUserAnswers(userAnswers)

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

    testControllerAuth(request())
    testSessionDataHandling(request())
    testLeppDataHandling(request())

    "errors exist in supplied data" should {
      def testErrorScenario(scenarioName: String,
                            accountName: Option[String],
                            sortCode: Option[String],
                            accountNumber: Option[String],
                            rollNumber: Option[String]): Unit = s"handle correctly for scenario - $scenarioName" in {
        mockAuthSuccess()

        val app: Application = applicationWithUserAnswers(userAnswersWithLepp)

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

    "a valid request is submitted" when {
      def handleForBarsResponse(scenarioName: String,
                                barsStatus: Int,
                                barsBody: String,
                                expectedRedirect: String,
                                barsRedirectHeader: Option[String]): Unit = s"handle for BARS scenario: $scenarioName" in {
        mockAuthSuccess()

        val formData: Seq[(String, String)] = Seq(
          "bankDetails.accountName" -> "Taxwell Payer",
          "bankDetails.sortCode" -> "11-22-33",
          "bankDetails.accountNumber" -> "12345678",
          "bankDetails.rollNumber" -> "1234/678"
        )

        val barsRequest: JsValue = Json.parse(
          """
            |{
            | "account": {
            |   "accountNumber":"12345678",
            |   "sortCode":"112233",
            |   "rollNumber":"1234678"
            | },
            | "subject": {
            |   "name":"Taxwell Payer"
            | }
            |}
            """.stripMargin
        )

        barsRedirectHeader.fold(
          when(method = POST, uri = "/verify/personal")
            .withRequestBody(barsRequest)
            .thenReturn(barsStatus, barsBody)
        )(rdr =>
          when(method = POST, uri = "/verify/personal")
            .withRequestBody(barsRequest)
            .thenReturn(barsStatus, Map(LOCATION -> rdr))
        )


        val app: Application = applicationWithUserAnswers(userAnswersWithLepp)

        val result: Future[Result] = route(app, request(formData: _*)).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(expectedRedirect)
      }

      def handleBarsWithNoRedirects(scenarioName: String,
                                    barsStatus: Int,
                                    barsBody: String,
                                    expectedRedirect: String): Unit =
        handleForBarsResponse(
          scenarioName = scenarioName,
          barsStatus = barsStatus,
          barsBody = barsBody,
          expectedRedirect = expectedRedirect,
          barsRedirectHeader = None
        )

      val checkFailedRoute: String = controllers.bars.routes.BarsCheckFailedController.onPageLoad().url
      val requestErrorsRoute: String = controllers.bars.routes.BarsRequestErrorsController.onPageLoad().url

      "BARS returns an error status" should {
        Seq(
          ("BARS returns 500 status", INTERNAL_SERVER_ERROR, "n/a", checkFailedRoute, None),
          ("BARS returns 404 status", NOT_FOUND, "n/a", checkFailedRoute, None),
          ("BARS returns 403 status", FORBIDDEN, "n/a", checkFailedRoute, None),
          ("BARS returns 400 status", BAD_REQUEST, "n/a", checkFailedRoute, None),
          ("BARS returns 301 status", MOVED_PERMANENTLY, "n/a", checkFailedRoute, Some("url")),
          ("BARS returns 303 status", SEE_OTHER, "n/a", checkFailedRoute, Some("url")),
          ("BARS returns 307 status", TEMPORARY_REDIRECT, "n/a", checkFailedRoute, Some("url")),
          ("BARS returns unhandled status", IM_A_TEAPOT, "n/a", checkFailedRoute, None)
        ).foreach(handleForBarsResponse)
      }

      "BARS returns a success response" should {
        def barsResponse(isWellFormatted: String = "yes",
                         accountExists: String = "yes",
                         nameMatches: String = "yes",
                         nonStandard: String = "no",
                         sortCodeFound: String = "yes",
                         supportsDirectCredit: String = "yes"): String =
          s"""
             |{
             | "accountNumberIsWellFormatted": "$isWellFormatted",
             | "accountExists": "$accountExists",
             | "nameMatches": "$nameMatches",
             | "accountName": "name",
             | "nonStandardAccountDetailsRequiredForBacs": "$nonStandard",
             | "sortCodeIsPresentOnEISCD": "$sortCodeFound",
             | "sortCodeSupportsDirectDebit": "yes",
             | "sortCodeSupportsDirectCredit": "$supportsDirectCredit",
             | "sortCodeBankName": "bank name",
             | "iban": "iban"
             |}
          """.stripMargin

        val errorsBarsBody: String = barsResponse(
          accountExists = "error",
          nameMatches = "error",
          sortCodeFound = "error",
          supportsDirectCredit = "error"
        )

        val indeterminateBarsBody: String = barsResponse(
          accountExists = "indeterminate",
          nameMatches = "indeterminate"
        )

        val multipleRequestErrorsBarsBody: String = barsResponse(
          supportsDirectCredit = "no",
          nonStandard = "yes"
        )

        val mixedErrorsBarsBody: String = barsResponse(
          supportsDirectCredit = "no",
          nonStandard = "yes",
          sortCodeFound = "error"
        )

        Seq(
          ("Account doesn't support direct credit", OK, barsResponse(supportsDirectCredit = "no"), requestErrorsRoute),
          ("Sort code not found", OK, barsResponse(sortCodeFound = "no"), requestErrorsRoute),
          ("Extra info required", OK, barsResponse(nonStandard = "yes"), requestErrorsRoute),
          ("Name doesn't match", OK, barsResponse(nameMatches = "no"), requestErrorsRoute),
          ("Account not found", OK, barsResponse(accountExists = "no"), requestErrorsRoute),
          ("Failed modulus check", OK, barsResponse(isWellFormatted = "no"), requestErrorsRoute),
          ("Field errors", OK, errorsBarsBody, checkFailedRoute),
          ("Result indeterminate", OK, indeterminateBarsBody, checkFailedRoute),
          ("Multiple request errors", OK, multipleRequestErrorsBarsBody, requestErrorsRoute),
          ("Mixed errors", OK, mixedErrorsBarsBody, checkFailedRoute)
        ).foreach(handleBarsWithNoRedirects)
      }
    }
  }

  "POST /change-bank-details" when {
    "errors in request" should {
      "load page with correct back links" in {
        def request(data: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] = FakeRequest(
          method = "POST",
          path = "/low-earners-pensions-payment/change-bank-details"
        )
          .withSession(SessionKeys.authToken -> "auth token")
          .withFormUrlEncodedBody(data: _*)

        mockAuthSuccess()

        val app: Application = applicationWithUserAnswers(userAnswersWithLepp)

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
  }
}