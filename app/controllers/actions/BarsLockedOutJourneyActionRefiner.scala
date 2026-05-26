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

import com.google.inject.ImplementedBy
import connectors.barsLockout.BarsVerifyStatusConnector
import connectors.barsLockout.model.BarVerifyStatusId
import controllers.actions.request.LockedOutJourneyRequest
import models.CorrelationId
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.mvc.{ActionRefiner, Result, Results}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.IdGenerator

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[BarsLockedOutJourneyActionRefiner])
trait BarsLockedOutJourneyAction extends ActionRefiner[IdentifierRequest, LockedOutJourneyRequest]

@Singleton
class BarsLockedOutJourneyActionRefiner @Inject() (
  barsVerifyStatusConnector: BarsVerifyStatusConnector,
  idGenerator: IdGenerator
)(implicit ec: ExecutionContext)
    extends BarsLockedOutJourneyAction
    with Logging
    with Results {

  override protected def refine[A](
    request: IdentifierRequest[A]
  ): Future[Either[Result, LockedOutJourneyRequest[A]]] = {

    implicit val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    implicit val correlationId: CorrelationId = CorrelationId(idGenerator.getCorrelationId)
    barsVerifyStatusConnector.status(BarVerifyStatusId.from(request.user.nino)).map { status =>
      status.lockoutExpiryDateTime match {
        case Some(expiry) =>
          Right(
            new LockedOutJourneyRequest(
              request = request,
              barsLockoutExpiryTime = expiry,
              numberOfBarsVerifyAttempts = status.attempts
            )
          )
        case None =>
          throw new RuntimeException(
            "Unexpected condition. This refiner should only be called when service is locked out"
          )
      }
    } recover { case e =>
      logger.error(
        s"[BarsLockedOutJourneyActionRefiner] failed to retrieve BarsVerifyStatus for nino=${request.user.nino.toString}, reason:${e.getMessage}"
      )
      Left(InternalServerError)
    }
  }

  override protected def executionContext: ExecutionContext = ec
}
