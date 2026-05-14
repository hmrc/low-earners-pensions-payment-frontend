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
import common.{IntegrationSpecBase, WireMockMethods}
import config.AppConfig
import models.userAnswers.LeppItemStatus.Available
import models.userAnswers.{BankAccountDetails, LeppItem, LeppSummary, UserAnswers}
import pages.TempPage.Dashboard
import play.api.Application
import play.api.http.Status.SEE_OTHER
import play.api.http.Writeable
import play.api.libs.json.*
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.NormalMode

import java.net.URLEncoder
import scala.concurrent.Future

trait ControllerIntegrationSpecBase extends IntegrationSpecBase with WireMockMethods {
  val summaryModel: LeppSummary = LeppSummary(
    currentLock = 67,
    items = Seq(
      LeppItem(
        taxYear = 2025,
        contributions = 1000,
        taxRate = 20,
        entitlement = 200,
        status = Available
      )
    )
  )

  val bankAccountDetails: BankAccountDetails = BankAccountDetails(
    accountName = "name",
    accountNumber = "number",
    sortCode = "sortcode",
    rollNumber = Some("rollNumber")
  )

  val userAnswers: UserAnswers = UserAnswers(
    id = "1",
    data = Json.obj(
      "leppSummary" -> Json.toJson(summaryModel),
      "bankDetails" -> Json.toJson(bankAccountDetails)
    )
  )
  
  private val authoriseUri: String = "/auth/authorise"

  private val authRequestJson: JsValue = Json.parse(
    """
      |{
      | "authorise": [
      |   {
      |      "identifiers": [],
      |      "state": "Activated",
      |      "enrolment": "HMRC-PI"
      |   }
      | ],
      | "retrieve": [
      |   "internalId",
      |   "nino",
      |   "confidenceLevel",
      |   "authorisedEnrolments"
      | ]
      |}
    """.stripMargin
  )

  private val ptaEnrolment: JsValue = Json.parse(
    """
      |{
      | "state": "Activated",
      | "key": "HMRC-PI"
      |}
    """.stripMargin
  )

  def mockAuthSuccess(): StubMapping = {
    val authResponseJson: JsObject =
      Json.obj("confidenceLevel" -> 250) ++
        Json.obj("nino" -> validNino()) ++
        Json.obj("internalId" -> "anId") ++
        Json.obj("authorisedEnrolments" -> JsArray(Seq(ptaEnrolment)))

    when(method = POST, uri = authoriseUri)
      .withRequestBody(authRequestJson)
      .thenReturn(status = OK, body = authResponseJson)
  }

  private def handleForAuthError[A: Writeable](request: FakeRequest[A],
                                               error: String,
                                               expectedRedirect: String): Unit =
    s"return expected result for auth error - $error" in {
      val errorString = s"""MDTP detail=\"$error\""""

      when(method = POST, uri = authoriseUri)
        .withRequestBody(authRequestJson)
        .thenReturn(status = UNAUTHORIZED, headers = Map("WWW-Authenticate" -> errorString))

      lazy val application: Application = fakeApplication()

      lazy val result: Future[Result] = route(application, request).getOrElse(
        Future.failed(new RuntimeException("TEST_ERROR"))
      )

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).getOrElse("N/A") shouldBe expectedRedirect
    }

  private def handleForAuthRedirect[A: Writeable](request: FakeRequest[A])
                                                 (scenarioName: String,
                                                  internalIdOpt: Option[String],
                                                  ninoOpt: Option[String],
                                                  confidenceLevel: Int,
                                                  enrolments: Seq[JsValue],
                                                  expectedRedirect: String): Unit =
    s"return expected result for scenario - $scenarioName" in {
      val authResponseJson: JsObject =
        Json.obj("confidenceLevel" -> confidenceLevel) ++
          ninoOpt.map(nino => Json.obj("nino" -> nino)).getOrElse(JsObject.empty) ++
          internalIdOpt.map(id => Json.obj("internalId" -> id)).getOrElse(JsObject.empty) ++
          Json.obj("authorisedEnrolments" -> JsArray(enrolments))

      when(method = POST, uri = authoriseUri)
        .withRequestBody(authRequestJson)
        .thenReturn(status = OK, body = authResponseJson)

      val application: Application = fakeApplication()

      lazy val result: Future[Result] = route(application, request).getOrElse(
        Future.failed(new RuntimeException("TEST_ERROR"))
      )

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).getOrElse("N/A") shouldBe expectedRedirect
    }

  def testControllerAuth[A: Writeable](request: FakeRequest[A]): Unit = {
    "user authorisation is run" when {
      "an authorisation error occurs" should {
        val config: AppConfig = fakeApplication().injector.instanceOf[AppConfig]
        val loginUrl: String = config.loginUrl + "?continue=" + URLEncoder.encode(
          config.loginContinueUrl,
          "UTF-8"
        )

        Seq(
          ("InvalidBearerToken", loginUrl),
          ("InternalError", controllers.auth.routes.UnauthorisedController.onPageLoad().url)
        ).foreach((error, redirect) => handleForAuthError(request, error, redirect))
      }

      "authorisation request succeeds" should {
        val unauthorisedUrl: String = controllers.auth.routes.UnauthorisedController.onPageLoad().url
        val ivUpliftUrl: String = fakeApplication().injector.instanceOf[AppConfig].ivUpliftUrl

        Seq(
          ("internalId is missing", None, Some(validNino()), 250, Seq(ptaEnrolment), unauthorisedUrl),
          ("nino is missing", Some("id"), None, 250, Seq(ptaEnrolment), unauthorisedUrl),
          ("confidenceLevel is too low", Some("id"), Some(validNino()), 50, Seq(ptaEnrolment), ivUpliftUrl),
          ("PTA enrolment is missing", Some("id"), Some(validNino()), 250, Nil, unauthorisedUrl)
        ).foreach(
          (sn, id, nino, cl, enrls, rdr) => handleForAuthRedirect(request)(sn, id, nino, cl, enrls, rdr)
        )
      }
    }
  }

  def testSessionDataHandling[A: Writeable](request: FakeRequest[A]): Unit = {
    "an LEPP submission has already been made for the current session" should {
      "wipe user answers and redirect to dashboard page" in {
        mockAuthSuccess()

        val summaryModel: LeppSummary = LeppSummary(
          currentLock = 67,
          items = Seq(
            LeppItem(
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Available
            )
          )
        )

        val bankAccountDetails: BankAccountDetails = BankAccountDetails(
          accountName = "name",
          accountNumber = "number",
          sortCode = "sortcode",
          rollNumber = Some("rollNumber")
        )

        val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = Json.obj(
            "leppSummary" -> Json.toJson(summaryModel),
            "bankDetails" -> Json.toJson(bankAccountDetails),
            "isSubmitted" -> JsBoolean(true)
          )
        )

        lazy val application: Application = applicationWithUserAnswers(userAnswers)

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.TempLeppController.onPageLoad().url)
      }
    }
  }

  def testLeppDataHandling[A: Writeable](request: FakeRequest[A]): Unit = {
    "LEPP data cannot be found in the session" should {
      "redirect to dashboard page" in {
        mockAuthSuccess()
        
        lazy val application: Application = applicationWithUserAnswers(
          UserAnswers(
            id = "1",
            data = JsObject.empty
          )
        )

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.TempLeppController.onPageLoad().url)
      }
    }
  }

  def testBankDetailsHandling[A: Writeable](request: FakeRequest[A]): Unit = {
    "bank details cannot be found in the session" should {
      "redirect to dashboard page" in {
        mockAuthSuccess()

        val summaryModel: LeppSummary = LeppSummary(
          currentLock = 67,
          items = Seq(
            LeppItem(
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Available
            )
          )
        )

        lazy val application: Application = applicationWithUserAnswers(
          UserAnswers(
            id = "1",
            data = JsObject(Seq(
              "leppSummary" -> Json.toJson(summaryModel)
            ))
          )
        )

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(routes.WhatAreYourBankDetailsController.onPageLoad(NormalMode).url)
      }
    }
  }
}
