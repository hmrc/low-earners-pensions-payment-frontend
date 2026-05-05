/*
 * Copyright 2023 HM Revenue & Customs
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

import connectors.barsLockout.BarsVerifyStatusConnector
import connectors.barsLockout.model.BarVerifyStatusId
import controllers.actions.request.BarsVerifiedRequest
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.mvc.{ActionRefiner, Result, Results}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BarsLockoutActionRefiner @Inject() (
  barsVerifyStatusConnector: BarsVerifyStatusConnector
)(implicit ec: ExecutionContext)
    extends ActionRefiner[IdentifierRequest, BarsVerifiedRequest]
    with Logging
    with Results {

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, BarsVerifiedRequest[A]]] = {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    println(" -------------------------- YES NO "+headerCarrier.authorization.get.value)

    barsVerifyStatusConnector.status(BarVerifyStatusId.from(request.user.nino)).map { status =>
      status.lockoutExpiryDateTime match {
        case Some(_) =>
          Left(Redirect(controllers.bars.routes.BarsLockoutController.barsLockout))
        case None =>
          Right(
            new BarsVerifiedRequest(
              request = request,
              numberOfBarsVerifyAttempts = status.attempts
            )
          )
      }
    } recover { case e =>
      logger.error(
        s"[BarsLockoutActionRefiner] failed to retrieve BarsVerifyStatus for nino=${request.user.nino.toString}, reason:${e.getMessage}"
      )
      Left(InternalServerError)
    }
  }

  override protected def executionContext: ExecutionContext = ec
}
