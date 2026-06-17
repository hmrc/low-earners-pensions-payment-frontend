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

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import connectors.{AcceptLeppPaymentConnector, ConnectorResponse}
import models.ResponseWrapper.SuccessWrapper
import models.backend.accept.*
import models.userAnswers.{BankAccountDetails, LeppItem, LeppSummary}
import models.{CorrelationId, ResponseWrapper}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LeppSubmissionService @Inject()(connector: AcceptLeppPaymentConnector) {
  protected[services] def submitSingle(acceptLeppPaymentRequest: AcceptLeppPaymentRequest)
                                      (implicit hc: HeaderCarrier,
                                       ec: ExecutionContext,
                                       cid: CorrelationId): ConnectorResponse[AcceptLeppPaymentResponse] = {
    connector.acceptPayment(acceptLeppPaymentRequest).map(success =>
        if (success.correlationId.value == "NO_CORRELATION_ID_IN_RESPONSE")
          success.copy(correlationId = cid)
        else
          success
    )
  }

  def submitMultiple(nino: Nino, bankAccountDetails: BankAccountDetails, leppSummary: LeppSummary)
                    (implicit hc: HeaderCarrier,
                     ec: ExecutionContext,
                     cid: CorrelationId): ConnectorResponse[AcceptLeppPaymentResponse] = {
    def doSubmit(nino: Nino,
                 bankAccountDetails: BankAccountDetails,
                 currentLeppLock: BigInt,
                 toSubmit: Seq[LeppItem])
                (implicit hc: HeaderCarrier,
                 ec: ExecutionContext,
                 cid: CorrelationId): ConnectorResponse[AcceptLeppPaymentResponse] = {
      toSubmit match {
        case Nil => EitherT(Future.successful(Right(
          SuccessWrapper(value = AcceptLeppPaymentResponse(currentLeppLock), correlationId = cid)
        )))
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
            doSubmit(
              nino = nino,
              bankAccountDetails = bankAccountDetails,
              currentLeppLock = success.value.updatedLowEarnersOptimisticLock,
              toSubmit = remainingItems
            )
          )
      }
    }
    
    doSubmit(
      nino = nino,
      bankAccountDetails = bankAccountDetails,
      currentLeppLock = leppSummary.currentLock,
      toSubmit = leppSummary.availableItems.getOrElse(Nil)
    )
  }
}
