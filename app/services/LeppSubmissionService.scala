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

package services

import com.google.inject.{Inject, Singleton}
import connectors.AcceptLeppPaymentConnector
import models.ResponseWrapper.SuccessWrapper
import models.backend.accept.*
import models.userAnswers.{BankAccountDetails, LeppSummary}
import models.{CorrelationId, ResponseWrapper}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import utils.Constants

import scala.+:
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LeppSubmissionService @Inject()(connector: AcceptLeppPaymentConnector) {
  protected[services] def submitSingle(acceptLeppPaymentRequest: AcceptLeppPaymentRequest)
                                      (implicit hc: HeaderCarrier,
                                       ec: ExecutionContext,
                                       cid: CorrelationId): Future[ResponseWrapper[AcceptLeppPaymentResponse]] =
    connector.acceptPayment(acceptLeppPaymentRequest).value.map {
      case Right(success) if success.correlationId.value == Constants.noCorrelationIdString => success.copy(correlationId = cid)
      case Right(success) => success
      case Left(e) => SuccessWrapper(AcceptLeppPaymentResponse(0), e.correlationId)
    }

  def submitMultiple(nino: Nino, bankAccountDetails: BankAccountDetails, leppSummary: LeppSummary)
                    (implicit hc: HeaderCarrier,
                     ec: ExecutionContext,
                     cid: CorrelationId): Future[ResponseWrapper[LeppSummary]] = {
    def doSubmit(nino: Nino,
                 bankAccountDetails: BankAccountDetails,
                 currentLeppLock: BigInt,
                 toSubmit: LeppSummary)
                (implicit hc: HeaderCarrier,
                 ec: ExecutionContext,
                 cid: CorrelationId): Future[ResponseWrapper[LeppSummary]] = {
      toSubmit.availableItems.getOrElse(Nil) match {
        case Nil =>
          val remainingAvailableItems = leppSummary.availableItems.getOrElse(Nil)
            .filterNot(item => toSubmit.acceptedItems.getOrElse(Nil).contains(item))
          Future.successful(SuccessWrapper(toSubmit.copy(availableItems = Some(remainingAvailableItems)), correlationId = cid))
        case nextItem +: remainingItems =>
          val acceptLeppPaymentRequest: AcceptLeppPaymentRequest = AcceptLeppPaymentRequest(
            identifier = nino,
            taxYear = nextItem.taxYear,
            body = AcceptLeppPaymentRequestBody(
              currentLowEarnersOptimisticLock = currentLeppLock,
              lowEarnersAccountDetails = bankAccountDetails
            )
          )

          submitSingle(acceptLeppPaymentRequest).flatMap(success =>
            implicit val newCid: CorrelationId = success.correlationId
            val (accepted, available) =
              if(success.value.updatedLowEarnersOptimisticLock == 0) (Some(toSubmit.acceptedItems.getOrElse(Nil)), None)
              else (Some(toSubmit.acceptedItems.getOrElse(Nil) :+ nextItem), Some(remainingItems))
            doSubmit(
              nino = nino,
              bankAccountDetails = bankAccountDetails,
              currentLeppLock = success.value.updatedLowEarnersOptimisticLock,
              toSubmit = leppSummary.copy(currentLock = success.value.updatedLowEarnersOptimisticLock,
                availableItems = available,
                acceptedItems = accepted)
            )
          )
      }
    }

    doSubmit(
      nino = nino,
      bankAccountDetails = bankAccountDetails,
      currentLeppLock = leppSummary.currentLock,
      toSubmit = leppSummary
    )
  }
}
