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
import connectors.{ConnectorResponse, PlaceholderBackendConnector}
import models.CorrelationId
import models.ResponseWrapper.SuccessWrapper
import models.backend.{SubmitLeppRequest, SubmitLeppResponse}
import models.userAnswers.{BankAccountDetails, LeppItem, LeppSummary}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LeppSubmissionService @Inject()(placeholderBackendConnector: PlaceholderBackendConnector) {
  protected[services] def submitSingle(currentLeppLock: BigInt, taxYear: Int, bankDetails: BankAccountDetails)
                                      (implicit hc: HeaderCarrier,
                                       ec: ExecutionContext,
                                       cid: CorrelationId): ConnectorResponse[SubmitLeppResponse] = {
    placeholderBackendConnector.submitLepp(
      SubmitLeppRequest(
        currentLowEarnersOptimisticLock = currentLeppLock,
        taxYear = taxYear,
        accountDetails = bankDetails
      )
    )
  }

  def submitMultiple(leppSummary: LeppSummary, accountDetails: BankAccountDetails)
                    (implicit hc: HeaderCarrier,
                     ec: ExecutionContext,
                     cid: CorrelationId): ConnectorResponse[SubmitLeppResponse] = {

    def doSubmit(currentLeppLock: BigInt, toSubmit: Seq[LeppItem])
                (implicit hc: HeaderCarrier,
                 ec: ExecutionContext,
                 cid: CorrelationId): ConnectorResponse[SubmitLeppResponse] = {
      toSubmit match {
        case Nil => EitherT(Future.successful(Right(
          SuccessWrapper(value = SubmitLeppResponse(currentLeppLock), correlationId = cid)
        )))
        case nextItem +: remainingItems =>
          submitSingle(currentLeppLock, nextItem.taxYear, accountDetails).flatMap(success =>
            implicit val cid: CorrelationId = success.correlationId
            doSubmit(
              currentLeppLock = success.value.updatedLowEarnersOptimisticLock,
              toSubmit = remainingItems
            )
          )
      }
    }
    
    doSubmit(leppSummary.currentLock, leppSummary.availableItems.getOrElse(Nil))
  }
}
