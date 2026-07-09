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

import com.google.inject.{ImplementedBy, Inject, Singleton}
import config.AppConfig
import connectors.UserAllowListConnector
import controllers.routes
import models.requests.{AuthUser, IdentifierRequest}
import play.api.mvc.*
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{ItmpName, Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.{Constants, Logging, MethodContext}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AuthenticatedIdentifierAction])
trait IdentifierAction extends ActionBuilder[IdentifierRequest, AnyContent]

@Singleton
class AuthenticatedIdentifierAction @Inject()(override val authConnector: AuthConnector,
                                              userAllowListConnector: UserAllowListConnector,
                                              config: AppConfig,
                                              playBodyParsers: BodyParsers.Default)
                                             (implicit override val executionContext: ExecutionContext)
  extends IdentifierAction with AuthorisedFunctions with Logging:

  override def parser: BodyParser[AnyContent] = playBodyParsers

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {
    given mc: MethodContext = MethodContext("invokeBlock")
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val retrievals: Retrieval[Option[String] ~ Option[String] ~ ConfidenceLevel ~ Enrolments ~ Option[ItmpName]] =
      Retrievals.internalId and 
        Retrievals.nino and
        Retrievals.confidenceLevel and
        Retrievals.authorisedEnrolments and
        Retrievals.itmpName
    
    authorised(Enrolment(Constants.ptaEnrolmentKey))
      .retrieve(retrievals) {
        case Some(internalId) ~ Some(nino) ~ confidenceLevel ~ enrolments ~ nameOpt if hasEnrolments(enrolments) =>
          if(confidenceLevel >= config.confidenceLevelMinimum) {
            isValidUser(IdentifierRequest(request, AuthUser(internalId, nino, nameOpt)), block)
          } else {
            logger.info("User has insufficient confidence level. Redirecting to IV uplift journey")
            Future.successful(Redirect(config.ivUpliftUrl))
          }
        case _ =>
          logger.info("User doesn't have PTA enrolment, not authorised to access this service.")
          Future.successful(Redirect(controllers.auth.routes.UnauthorisedController.onPageLoad()))
      } recoverWith {
      case _: NoActiveSession =>
        Future.successful(Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl))))
      case err: AuthorisationException =>
        logger.underlying.error(s"[${logger.cc}][$mc] - " + s"An authorisation error occurred with message", err)
        Future.successful(Redirect(controllers.auth.routes.UnauthorisedController.onPageLoad()))
    }
  }

  private def isValidUser[A](request: IdentifierRequest[A], block: IdentifierRequest[A] => Future[Result])
                            (implicit hc: HeaderCarrier): Future[Result] =
    if (config.privateBetaEnabled) {
      userAllowListConnector.check("nino", request.user.nino.value) flatMap {
        case true => block(request)
        case false => Future.successful(Redirect(routes.PrivateBetaUnauthorisedController.onPageLoad()))
      }
    } else {
      block(request)
    }
    
  private def hasEnrolments(enrolments: Enrolments): Boolean =
    enrolments.getEnrolment(Constants.ptaEnrolmentKey).nonEmpty
