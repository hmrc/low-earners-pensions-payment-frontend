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
import connectors.UserAllowListConnector
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Application
import play.api.mvc.*
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, StubPlayBodyParsersFactory}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{ItmpName, Retrieval, ~}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.HeaderCarrier
import utils.Constants

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class IdentifierActionSpec extends SpecBase with StubPlayBodyParsersFactory {
  private val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private val mockUserAllowListConnector: UserAllowListConnector = mock[UserAllowListConnector]

  private val application: Application = applicationBuilder(userAnswers = emptyUserAnswers).build()
  private val appConfig: AppConfig = application.injector.instanceOf[AppConfig]
  private val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

  private type AuthType = Option[String] ~ Option[String] ~ ConfidenceLevel ~ Enrolments ~ Option[ItmpName]

  def authResult(internalId: Option[String],
                 nino: Option[String],
                 confidenceLevel: ConfidenceLevel,
                 enrolments: Enrolment*): AuthType =
    internalId and nino and confidenceLevel and Enrolments(enrolments.toSet) and Option.empty[ItmpName]

  val ptaEnrolment: Enrolment = Enrolment(
    key = Constants.ptaEnrolmentKey,
    identifiers = Seq(EnrolmentIdentifier(key = "Some_Id", value = "A2100001")), state = "Activated"
  )

  val invalidEnrolment: Enrolment = Enrolment(
    key = "INVALID",
    identifiers = Seq.empty,
    state = "Activated"
  )

  def setAuthValue[A](value: Future[A]): Unit =
    when(
      mockAuthConnector.authorise[A](
        predicate = any(),
        retrieval = any()
      )(
        hc = any(),
        ec = any()
      )
    ).thenReturn(value)

  def setAuthValue(value: AuthType): Unit = setAuthValue(Future.successful(value))

  class Harness(authAction: IdentifierAction) {
    def onPageLoad(): Action[AnyContent] = authAction { _ => Results.Ok }
  }

  private def authTest(internalId: Option[String] = Some("internalId"),
                       nino: Option[String] = Some(generateNino()),
                       confidenceLevel: ConfidenceLevel = ConfidenceLevel.L250,
                       enrolments: Seq[Enrolment] = Seq(ptaEnrolment))
                      (expectedStatus: Int,
                       expectedRedirectOpt: Option[String]): Unit = {
    setAuthValue(authResult(internalId, nino, confidenceLevel, enrolments: _*))

    running(application) {
      val authAction: AuthenticatedIdentifierAction = new AuthenticatedIdentifierAction(
        authConnector = mockAuthConnector,
        userAllowListConnector = mockUserAllowListConnector,
        config = appConfig,
        playBodyParsers = bodyParsers
      )

      val controller: Harness = new Harness(authAction)
      val result: Future[Result] = controller.onPageLoad()(FakeRequest())

      status(result) mustBe expectedStatus
      redirectLocation(result) mustBe expectedRedirectOpt
    }
  }

  "IdentifierAction" - {
    "when authorisation request completes" - {
      "if InternalId cannot be retrieved from auth should redirect to Unauthorised page" in {
        authTest(
          internalId = None
        )(
          expectedStatus = SEE_OTHER,
          expectedRedirectOpt = Some(controllers.auth.routes.UnauthorisedController.onPageLoad().url)
        )
      }

      "if NINO cannot be retrieved from auth should redirect to wrong account type page" in {
        authTest(
          nino = None
        )(
          expectedStatus = SEE_OTHER,
          expectedRedirectOpt = Some(controllers.auth.routes.WrongAccountUnauthorisedController.onPageLoad().url)
        )
      }

      "if PTA enrolment is missing from auth response should redirect to PTA" in {
        authTest(
          enrolments = Nil
        )(
          expectedStatus = SEE_OTHER,
          expectedRedirectOpt = Some(appConfig.ptaUrl)
        )
      }

      "if PTA enrolment is inactive should redirect to PTA" in {
        authTest(
          enrolments = Seq(ptaEnrolment.copy(state = "Inactive"))
        )(
          expectedStatus = SEE_OTHER,
          expectedRedirectOpt = Some(appConfig.ptaUrl)
        )
      }

      "if confidence level is less than 250 should redirect to IV uplift" in {
        authTest(
          confidenceLevel = ConfidenceLevel.L50
        )(
          expectedStatus = SEE_OTHER,
          expectedRedirectOpt = Some(appConfig.ivUpliftUrl)
        )
      }

      "for happy path scenario should invoke block" in {
        authTest()(
          expectedStatus = OK,
          expectedRedirectOpt = None
        )
      } 
    }

    class FakeFailingAuthConnector @Inject()(exceptionToReturn: Throwable) extends AuthConnector {
      val serviceUrl: String = ""

      override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])
                               (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
        Future.failed(exceptionToReturn)
    }
    
    "when exceptions occur during authorisation" - {
      "if the user has no active session should redirect to login url" in {
        running(application) {
          val authAction: AuthenticatedIdentifierAction = new AuthenticatedIdentifierAction(
            authConnector = new FakeFailingAuthConnector(BearerTokenExpired()),
            userAllowListConnector = mockUserAllowListConnector,
            config = appConfig,
            playBodyParsers = bodyParsers
          )

          val controller: Harness = new Harness(authAction)
          val result: Future[Result] = controller.onPageLoad()(FakeRequest())

          val loginUrl: String = urlEncode(appConfig.loginContinueUrl)
          val expectedUrl: String = s"${appConfig.loginUrl}?continue=$loginUrl"

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(expectedUrl)
        }
      }

      "if an unhandled authorisation exception occurs should redirect to default error page" in {
        running(application) {
          val authAction: AuthenticatedIdentifierAction = new AuthenticatedIdentifierAction(
            authConnector = new FakeFailingAuthConnector(InternalError()),
            userAllowListConnector = mockUserAllowListConnector,
            config = appConfig,
            playBodyParsers = bodyParsers
          )

          val controller: Harness = new Harness(authAction)
          val result: Future[Result] = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(controllers.routes.SomethingWentWrongController.onPageLoad().url)
        }
      }
      
      "if any non-authorisation exception occurs should throw exception" in {
        running(application) {
          val authAction: AuthenticatedIdentifierAction = new AuthenticatedIdentifierAction(
            authConnector = new FakeFailingAuthConnector(new RuntimeException()),
            userAllowListConnector = mockUserAllowListConnector,
            config = appConfig,
            playBodyParsers = bodyParsers
          )

          val controller: Harness = new Harness(authAction)
          assertThrows[RuntimeException](await(controller.onPageLoad()(FakeRequest())))
        }
      }
    }
  }
}
