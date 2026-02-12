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

package controllers.auth

import base.SpecBase
import config.AppConfig
import controllers.actions.{DataRetrievalAction, FakeDataRetrievalAction, IdentifierAction}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import java.net.URLEncoder

class AuthControllerSpec extends SpecBase with MockitoSugar {

  "signOut" - {

    "must clear user answers and redirect to sign out, specifying the exit survey as the continue URL" in {

      val application =
        new GuiceApplicationBuilder().configure().overrides(
            bind[IdentifierAction].toInstance(fakeIdentifierAction),
            bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(emptyUserAnswers)))
          .build()

      running(application) {

        val appConfig = application.injector.instanceOf[AppConfig]
        val request   = FakeRequest(GET, routes.AuthController.signOut().url)

        val result = route(application, request).value
        val expectedRedirectUrl = s"${appConfig.exitSurveyUrl}"

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual expectedRedirectUrl
      }
    }
  }

  "sessionTimeout" - {
    "must clear users answers and redirect to sign out, specifying SessionTimeout as the continue URL" in {

      val application =
        applicationBuilder()
          .build()

      running(application) {
        val appConfig = application.injector.instanceOf[AppConfig]
        val request   = FakeRequest(GET, routes.AuthController.sessionTimeout().url)

        val result = route(application, request).value

        val encodedContinueUrl  = URLEncoder.encode(appConfig.host + controllers.auth.routes.SessionTimeoutController.onPageLoad().url, "UTF-8")
        val expectedRedirectUrl = s"${appConfig.signOutUrl}?continue=$encodedContinueUrl&origin=${appConfig.appName}"

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual expectedRedirectUrl
      }
    }
  }
}