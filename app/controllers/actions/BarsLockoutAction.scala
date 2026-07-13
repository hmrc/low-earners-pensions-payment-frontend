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

import connectors.BarsVerifyStatusConnector
import models.CorrelationId
import models.barsLockout.BarsVerifyStatusResponse
import models.requests.{BarsVerifiedRequest, IdentifierRequest}
import play.api.Logging
import play.api.mvc.{ActionRefiner, Result, Results}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.CorrelationIdHandler

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait BarsLockoutAction extends ActionRefiner[IdentifierRequest, BarsVerifiedRequest] with Logging with Results {
  val barsVerifyStatusConnector: BarsVerifyStatusConnector
  val correlationIdHandler: CorrelationIdHandler

  def handleWithBarsStatus[A](f: BarsVerifyStatusResponse => Either[Result, BarsVerifiedRequest[A]])
                             (using cid: CorrelationId)
                             (using HeaderCarrier, ExecutionContext): Future[Either[Result, BarsVerifiedRequest[A]]] = {

    barsVerifyStatusConnector
      .status()
      .map(status => f(status))
      .recover { case e =>
        logger.error(
          s"[BarsLockoutActionRefiner] " +
            s"failed to retrieve BarsVerifyStatus for " +
            s"correlationId: $cid, " +
            s"reason:${e.getMessage}"
        )
        Left(InternalServerError)
      }
  }
}

@Singleton
class NoRedirectBarsLockoutAction @Inject()(val barsVerifyStatusConnector: BarsVerifyStatusConnector,
                                            val correlationIdHandler: CorrelationIdHandler)
                                           (implicit ec: ExecutionContext)
  extends BarsLockoutAction {

  override protected[actions] def refine[A](request: IdentifierRequest[A]): Future[Either[Result, BarsVerifiedRequest[A]]] = {
    given hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    given cid: CorrelationId = correlationIdHandler.getCorrelationId(request.request)

    handleWithBarsStatus { status =>
      Right(
        new BarsVerifiedRequest(
          request = request.copy(request = ActionUtils.requestWithCid(request.request)),
          barsLockoutExpiryOpt = status.lockoutExpiryDateTime
        )
      )
    }
  }

  override protected def executionContext: ExecutionContext = ec
}

@Singleton
class RedirectBarsLockoutAction @Inject()(val barsVerifyStatusConnector: BarsVerifyStatusConnector,
                                          val correlationIdHandler: CorrelationIdHandler)
                                         (implicit ec: ExecutionContext)
  extends BarsLockoutAction {

  override protected[actions] def refine[A](request: IdentifierRequest[A]): Future[Either[Result, BarsVerifiedRequest[A]]] = {
    given hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    given cid: CorrelationId = correlationIdHandler.getCorrelationId(request.request)

    handleWithBarsStatus { status =>
      status.lockoutExpiryDateTime match {
        case Some(_) => Left(
          Redirect(controllers.bars.routes.BarsLockoutController.onPageLoad())
        )
        case None => Right(
          new BarsVerifiedRequest(
            request = request.copy(request = ActionUtils.requestWithCid(request.request))
          )
        )
      }
    }
  }

  override protected def executionContext: ExecutionContext = ec
}
