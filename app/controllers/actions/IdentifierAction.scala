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
import models.requests.{AuthUser, IdentifierRequest}
import play.api.mvc.*
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.{Constants, Logging}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AuthenticatedIdentifierAction])
trait IdentifierAction extends ActionBuilder[IdentifierRequest, AnyContent]

@Singleton
class AuthenticatedIdentifierAction @Inject()(override val authConnector: AuthConnector,
                                              config: AppConfig,
                                              playBodyParsers: BodyParsers.Default)
                                             (implicit override val executionContext: ExecutionContext)
  extends IdentifierAction with AuthorisedFunctions with Logging:

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {
    val logContext: String = "[AuthenticatedIdentifierAction][invokeBlock] - "
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised(Enrolment(Constants.ptaEnrolmentKey))
      .retrieve(Retrievals.internalId and Retrievals.nino and Retrievals.confidenceLevel and Retrievals.authorisedEnrolments) {
        case Some(internalId) ~ Some(nino) ~ confidenceLevel ~ enrolments if hasEnrolments(enrolments) =>
          if(confidenceLevel >= config.confidenceLevelMinimum) {
            block(IdentifierRequest(request, AuthUser.apply(internalId, nino)))
          }else {
            logger.info("invokeBlock", "User has insufficient confidence level. Redirecting to IV uplift journey")
            Future.successful(Redirect(config.ivUpliftUrl))
          }
        case _ =>
          logger.info("invokeBlock", "User doesn't have PTA enrolment, not authorised to access this service.")
          Future.successful(Redirect(controllers.auth.routes.UnauthorisedController.onPageLoad()))
      } recoverWith {
      case _: NoActiveSession =>
        Future.successful(Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl))))
      case err: AuthorisationException =>
        logger.underlying.error(logContext + s"An authorisation error occurred with message", err)
        Future.successful(Redirect(controllers.auth.routes.UnauthorisedController.onPageLoad()))
    }
  }

  private def hasEnrolments(enrolments: Enrolments): Boolean =
    enrolments.getEnrolment(Constants.ptaEnrolmentKey).nonEmpty

  override def parser: BodyParser[AnyContent] = playBodyParsers

