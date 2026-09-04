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

import config.AppConfig
import play.api.Application
import play.api.http.Writeable
import play.api.libs.json.*
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.SessionKeys

import java.net.URLEncoder
import scala.concurrent.Future

class ControllerAuthISpec extends ControllerIntegrationSpecBase {

  val config: AppConfig = fakeApplication().injector.instanceOf[AppConfig]
  val loginUrl: String = config.loginUrl + "?continue=" + URLEncoder.encode(
    config.loginContinueUrl,
    "UTF-8"
  )

  val unauthorisedUrl: String = controllers.auth.routes.UnauthorisedController.onPageLoad().url
  val wrongAccountUnauthorisedUrl: String = controllers.auth.routes.WrongAccountUnauthorisedController.onPageLoad().url
  val ivUpliftUrl: String = fakeApplication().injector.instanceOf[AppConfig].ivUpliftUrl

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
                                                  expectedRedirect: String): Unit = {
    s"return expected result for scenario - $scenarioName" in {
      val authResponseJson: JsObject =
        Json.obj("confidenceLevel" -> confidenceLevel) ++
          ninoOpt.map(nino => Json.obj("nino" -> nino)).getOrElse(JsObject.empty) ++
          internalIdOpt.map(id => Json.obj("internalId" -> id)).getOrElse(JsObject.empty)

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
  }

  "Auth request for GET endpoints" should {
    Seq(
      "/accept-your-low-earners-pension-payment/start",
      "/accept-your-low-earners-pension-payment/payments",
      "/accept-your-low-earners-pension-payment/payment-breakdown",
      "/accept-your-low-earners-pension-payment/bank-details",
      "/accept-your-low-earners-pension-payment/bank-details-not-verified-user",
      "/accept-your-low-earners-pension-payment/bank-details-not-verified-service",
      "/accept-your-low-earners-pension-payment/check-your-answers",
      "/accept-your-low-earners-pension-payment/bank-details-received",
    ).foreach(url =>
      s"for GET of url: $url" when {
        Seq(
          ("InvalidBearerToken", loginUrl),
          ("InternalError", unauthorisedUrl),
          ("InsufficientEnrolments", wrongAccountUnauthorisedUrl)
        ).foreach((error, redirect) => handleForAuthError(
          FakeRequest(
            method = "GET",
            path = url
          ).withSession(SessionKeys.authToken -> "auth token"), error, redirect
        )
        )
      })
  }

  "Auth request for POST endpoints" should {
    Seq(
      "/accept-your-low-earners-pension-payment/bank-details",
      "/accept-your-low-earners-pension-payment/check-your-answers"
    ).foreach(url =>
      s"for POST of url: $url" when {
        Seq(
          ("internalId is missing", None, Some(validNino()), 250, unauthorisedUrl),
          ("nino is missing", Some("id"), None, 250, unauthorisedUrl),
          ("confidenceLevel is too low", Some("id"), Some(validNino()), 50, ivUpliftUrl)
        ).foreach(
          (sn, id, nino, cl, rdr) => handleForAuthRedirect(FakeRequest(
            method = "POST",
            path = url
          )
            .withSession(SessionKeys.authToken -> "auth token")
            .withFormUrlEncodedBody())(sn, id, nino, cl, rdr)
        )
      }
    )
  }
}
