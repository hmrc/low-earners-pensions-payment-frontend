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

package controllers.actions

import base.SpecBase
import com.google.inject.Inject
import config.AppConfig
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.mvc.*
import play.api.test.{FakeRequest, StubPlayBodyParsersFactory}
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.ConfidenceLevel.{L200, L250}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedIdentifierActionSpec extends SpecBase with StubPlayBodyParsersFactory {

  private val mockAuthConnector: AuthConnector = mock[AuthConnector]

  def authResult(internalId: Option[String],
                 nino: Option[String],
                 confidenceLevel: ConfidenceLevel): Option[String] ~ Option[String] ~ ConfidenceLevel =
    internalId and nino and confidenceLevel

  def setAuthValue(value: Option[String] ~ Option[String] ~ ConfidenceLevel): Unit =
    setAuthValue(Future.successful(value))

  def setAuthValue[A](value: Future[A]): Unit =
    when(mockAuthConnector.authorise[A](any(), any())(any(), any()))
      .thenReturn(value)

  class Harness(authAction: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction { _ => Results.Ok }
  }

  "Auth Action" - {
    "when the user logged in" - {
      "must return a valid IdentifierRequest" in {
        setAuthValue(authResult(Some("internalId"), Some("AA123456C"), L250))

        val application = applicationBuilder(userAnswers = emptyUserAnswers).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[AppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            authConnector = mockAuthConnector,
            config = appConfig,
            playBodyParsers = bodyParsers
          )

          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe OK
        }
      }
    }

    "when the user has confidence level less than 250" - {
      "must redirect to IV uplift journey" in {
        setAuthValue(authResult(Some("internalId"), Some("AA123456C"), L200))

        val application = applicationBuilder(userAnswers = emptyUserAnswers).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[AppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            authConnector = mockAuthConnector,
            config = appConfig,
            playBodyParsers = bodyParsers
          )

          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.ivUpliftUrl)
        }
      }
    }

    "when the user hasn't logged in" - {
      "must redirect the user to log in " in {
        val application = applicationBuilder(userAnswers = emptyUserAnswers).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[AppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            authConnector = new FakeFailingAuthConnector(BearerTokenExpired()),
            config = appConfig,
            playBodyParsers = bodyParsers
          )

          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user's session has expired" - {
      "must redirect the user to login page page" in {
        val application = applicationBuilder(userAnswers = emptyUserAnswers).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[AppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            authConnector = new FakeFailingAuthConnector(SessionRecordNotFound()),
            config = appConfig,
            playBodyParsers = bodyParsers
          )

          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())
          val continueUrl = urlEncode(appConfig.loginContinueUrl)
          val expectedUrl = s"${appConfig.loginUrl}?continue=$continueUrl"

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe expectedUrl
        }
      }
    }

    "the user has an unsupported affinity group" - {
      "must redirect the user to the sign in page" in {
        val application = applicationBuilder(userAnswers = emptyUserAnswers).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[AppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            authConnector = new FakeFailingAuthConnector(new UnsupportedAffinityGroup),
            config = appConfig,
            playBodyParsers = bodyParsers
          )

          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())
          val expectedUrl = controllers.auth.routes.UnauthorisedController.onPageLoad().url

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe expectedUrl
        }
      }
    }

    "any unhandled exception occurs" - {
      "must allow the exception to be thrown" in {
        val application = applicationBuilder(userAnswers = emptyUserAnswers).build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]
          val appConfig = application.injector.instanceOf[AppConfig]

          val authAction = new AuthenticatedIdentifierAction(
            authConnector = new FakeFailingAuthConnector(new RuntimeException()),
            config = appConfig,
            playBodyParsers = bodyParsers
          )

          val controller = new Harness(authAction)
          val result: Future[Result] = controller.onPageLoad()(FakeRequest())

          assertThrows[RuntimeException](
            await(result)
          )
        }
      }
    }
  }
}

class FakeFailingAuthConnector @Inject()(exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.failed(exceptionToReturn)
}
