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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.CorrelationId
import models.backend.accept.AcceptLeppPaymentRequestBody
import models.userAnswers.LeppItemStatus.{Available, Paid}
import models.userAnswers.{LeppItem, LeppSummary, UserAnswers}
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{route, writeableOf_AnyContentAsEmpty}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.http.SessionKeys
import viewmodels.NormalMode
import viewmodels.checkYourAnswers.CheckYourAnswersSummary.cyaSummaryList
import viewmodels.formPages.FormPageViewModel
import views.html.CheckYourAnswersView

import java.time.LocalDate
import scala.concurrent.Future

class CheckYourAnswersControllerISpec extends ControllerIntegrationSpecBase {
  
  val barsVerifyStatusUrl = ""
  "GET /check-your-answers" when {
    val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
      method = "GET",
      path = "/accept-your-low-earners-pension-payment/check-your-answers"
    ).withSession(SessionKeys.authToken -> "auth token")
    
    "a valid request is made" should {
      "return the expected view" in {
        mockAuthSuccess()
        mockBarsVerifyStatus(status = OK,
          response = Json.obj("attempts" -> 1))

        val application: Application = applicationWithUserAnswers(userAnswersWithBankDetails)

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        val view = application.injector.instanceOf[CheckYourAnswersView]
        implicit val messages: Messages = application.injector.instanceOf[MessagesApi].preferred(request)

        val rows: Seq[SummaryListRow] = cyaSummaryList(bankAccountDetails)
        val viewModel: FormPageViewModel = FormPageViewModel(
          onSubmit = routes.CheckYourAnswersController.onSubmit(),
          backLinkUrl = Some(routes.WhatAreYourBankDetailsController.onPageLoad(NormalMode).url)
        )

        status(result) shouldBe OK
        contentAsString(result) shouldEqual view(rows, viewModel)(request, messages).toString
      }
    }

    "user locked out for too many BARS attempts" should {
      "return bank details view" in {
        mockAuthSuccess()
        mockBarsVerifyStatus(
          status = OK,
          response = Json.obj("attempts" -> 3, "lockoutExpiryDateTime" -> "2020-12-26T00:00:00Z")
        )

        val application: Application = applicationWithUserAnswers(userAnswersWithBankDetails)

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(bars.routes.BarsLockoutController.onPageLoad().url)
      }
    }
  }

  "POST /check-your-answers" when {
    def request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
      method = "POST",
      path = "/accept-your-low-earners-pension-payment/check-your-answers"
    )
      .withSession(SessionKeys.authToken -> "auth token")
      .withHeaders("correlationId" -> testCorrelationId.value)

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

    def mockBarsSuccess(): StubMapping = {
      val barsResponse: String =
        s"""
           |{
           | "accountNumberIsWellFormatted": "yes",
           | "accountExists": "yes",
           | "nameMatches": "yes",
           | "accountName": "name",
           | "nonStandardAccountDetailsRequiredForBacs": "no",
           | "sortCodeIsPresentOnEISCD": "yes",
           | "sortCodeSupportsDirectDebit": "yes",
           | "sortCodeSupportsDirectCredit": "yes",
           | "sortCodeBankName": "bank name",
           | "iban": "iban"
           |}
        """.stripMargin

      when(method = POST, uri = "/verify/personal")
        .withRequestBody(barsRequest)
        .thenReturn(OK, barsResponse)
    }

    "BARS check fails" should {
      def handleForBarsResponse(scenarioName: String,
                                barsStatus: Int,
                                barsBody: String,
                                expectedRedirect: String,
                                barsRedirectHeader: Option[String] = None): Unit = s"handle for BARS scenario: $scenarioName" in {
        mockAuthSuccess()

        barsRedirectHeader.fold(
          when(method = POST, uri = "/verify/personal")
            .withRequestBody(barsRequest)
            .thenReturn(barsStatus, barsBody)
        )(rdr =>
          when(method = POST, uri = "/verify/personal")
            .withRequestBody(barsRequest)
            .thenReturn(barsStatus, Map(LOCATION -> rdr))
        )

        val app: Application = applicationWithUserAnswers(userAnswersWithBankDetails)

        val result: Future[Result] = route(app, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )
        mockBarsVerifyStatus(status = OK,
          response = Json.obj("attempts" -> 1))
        
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(expectedRedirect)
      }

      val checkFailedRoute: String = controllers.bars.routes.BarsCheckFailedController.onPageLoad().url
      val requestErrorsRoute: String = controllers.bars.routes.BarsRequestErrorsController.onPageLoad().url

      "the BARS service return an error response" must {
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

      "the BARS service return an OK response with business rule violations" should {
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
        ).foreach((scenarioName, barsStatus, barsBody, expectedRedirect) =>
          handleForBarsResponse(
            scenarioName = scenarioName,
            barsStatus = barsStatus,
            barsBody = barsBody,
            expectedRedirect = expectedRedirect,
            barsRedirectHeader = None
          )
        )
      }
    }

    val requestBody: AcceptLeppPaymentRequestBody = AcceptLeppPaymentRequestBody(
      currentLowEarnersOptimisticLock = 67,
      lowEarnersAccountDetails = bankAccountDetails
    )
    
    def mockBackendResponse(taxYear: Int,
                            requestBody: Option[String],
                            responseStatus: Int = CREATED,
                            responseBody: String): StubMapping = when(
      method = POST,
      uri = s"/low-earners-pensions-payment/accept-payment/$taxYear",
      headers = Map("correlationId" -> testCorrelationId),
      bodyOpt = requestBody
    ).thenReturn(
      status = responseStatus,
      body = responseBody,
      headers = Map("correlationId" -> testCorrelationId)
    )

    "backend returns an error for first LEPP submission" should {
      "redirect to ClearCacheController" in {
        mockAuthSuccess()
        mockBarsSuccess()
        mockBarsVerifyStatus(status = OK, response = Json.obj("attempts" -> 1))

        mockBackendResponse(
          taxYear = 2025,
          requestBody = Some(Json.toJson(requestBody).toString),
          responseStatus = IM_A_TEAPOT,
          responseBody = ""
        )
        
        val application: Application = applicationWithUserAnswers(userAnswersWithBankDetails)

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )
        
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.ClearCacheController.defaultError().url)
      }
    }

    "backend returns an error for a non-first LEPP submission" should {
      "redirect to SubmitConfirmationController" in {
        mockAuthSuccess()
        mockBarsSuccess()
        mockBarsVerifyStatus(status = OK, response = Json.obj("attempts" -> 1))

        val summaryModel: LeppSummary = LeppSummary(
          currentLock = 67,
          availableItems = Some(Seq(
            LeppItem(
              id = "id-1",
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Available,
              claimDate = None
            ),
            LeppItem(
              id = "id-2",
              taxYear = 2026,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Available,
              claimDate = None
            )
          )),
          paidItems = Some(Seq(
            LeppItem(
              id = "id-3",
              taxYear = 2024,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Paid,
              claimDate = Some(LocalDate.of(2025, 1, 1))
            )
          ))
        )

        val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = Json.obj(
            "leppSummary" -> Json.toJson(summaryModel),
            "bankDetails" -> Json.toJson(bankAccountDetails)
          )
        )

        val application: Application = applicationWithUserAnswers(userAnswers)

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        mockBackendResponse(
          taxYear = 2025,
          requestBody = Some(
            """
              |{
              | "currentLowEarnersOptimisticLock":67,
              | "lowEarnersAccountDetails":{
              |   "accountName":"Taxwell Payer",
              |   "accountNumber":"12345678",
              |   "sortCode":"112233",
              |   "rollNumber":"1234678"
              |   }
              |}
            """.stripMargin
          ),
          responseStatus = CREATED,
          responseBody =
            """
              |{
              | "updatedLowEarnersOptimisticLock": 68
              |}
            """.stripMargin
        )

        mockBackendResponse(
          taxYear = 2026,
          requestBody = Some(
            """
              |{
              | "currentLowEarnersOptimisticLock": 68,
              | "lowEarnersAccountDetails":{
              |   "accountName":"Taxwell Payer",
              |   "accountNumber":"12345678",
              |   "sortCode":"112233",
              |   "rollNumber":"1234678"
              | }
              |}
            """.stripMargin
          ),
          responseStatus = IM_A_TEAPOT,
          responseBody = "TEAPOT_TIME"
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.SubmitConfirmationController.onPageLoad().url)
      }
    }

    "submission succeeds for a single available LEPP item" should {
      "redirect to confirmation page" in {
        mockAuthSuccess()
        mockBarsSuccess()
        mockBarsVerifyStatus(status = OK, response = Json.obj("attempts" -> 1))

        mockBackendResponse(
          taxYear = 2025,
          requestBody = Some(
            """
              |{
              | "currentLowEarnersOptimisticLock":67,
              | "lowEarnersAccountDetails":{
              |   "accountName":"Taxwell Payer",
              |   "accountNumber":"12345678",
              |   "sortCode":"112233",
              |   "rollNumber":"1234678"
              |   }
              |}
            """.stripMargin
          ),
          responseStatus = CREATED,
          responseBody =
            """
              |{
              | "updatedLowEarnersOptimisticLock": 1234
              |}
            """.stripMargin
        )

        val application: Application = applicationWithUserAnswers(userAnswersWithBankDetails)

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.SubmitConfirmationController.onPageLoad().url)
      }
    }

    "submission succeeds for a multiple available LEPP items" should {
      "redirect to confirmation page" in {
        mockAuthSuccess()
        mockBarsSuccess()
        mockBarsVerifyStatus(status = OK, response = Json.obj("attempts" -> 1))

        val summaryModel: LeppSummary = LeppSummary(
          currentLock = 67,
          availableItems = Some(Seq(
            LeppItem(
              id = "id-1",
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Available,
              claimDate = None
            ),
            LeppItem(
              id = "id-2",
              taxYear = 2026,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Available,
              claimDate = None
            )
          )),
          paidItems = Some(Seq(
            LeppItem(
              id = "id-3",
              taxYear = 2024,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Paid,
              claimDate = Some(LocalDate.of(2025, 1, 1))
            )
          ))
        )

        val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = Json.obj(
            "leppSummary" -> Json.toJson(summaryModel),
            "bankDetails" -> Json.toJson(bankAccountDetails)
          )
        )

        val application: Application = applicationWithUserAnswers(userAnswers)

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        mockBackendResponse(
          taxYear = 2025,
          requestBody = Some(
            """
              |{
              | "currentLowEarnersOptimisticLock":67,
              | "lowEarnersAccountDetails":{
              |   "accountName":"Taxwell Payer",
              |   "accountNumber":"12345678",
              |   "sortCode":"112233",
              |   "rollNumber":"1234678"
              |   }
              |}
            """.stripMargin
          ),
          responseStatus = CREATED,
          responseBody =
          """
             |{
             | "updatedLowEarnersOptimisticLock": 68
             |}
            """.stripMargin
        )

        mockBackendResponse(
          taxYear = 2026,
          requestBody = Some(
            """
              |{
              | "currentLowEarnersOptimisticLock": 68,
              | "lowEarnersAccountDetails":{
              |   "accountName":"Taxwell Payer",
              |   "accountNumber":"12345678",
              |   "sortCode":"112233",
              |   "rollNumber":"1234678"
              | }
              |}
            """.stripMargin
          ),
          responseStatus = CREATED,
          responseBody =
            """
              |{
              | "updatedLowEarnersOptimisticLock": 69
              |}
            """.stripMargin
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.SubmitConfirmationController.onPageLoad().url)
      }
    }
  }

}
