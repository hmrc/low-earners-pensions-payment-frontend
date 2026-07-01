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
import connectors.{AcceptLeppPaymentConnector, ConnectorResponse, rawConnectorFailure, rawConnectorSuccess}
import models.CorrelationId
import models.backend.accept.*
import models.errors.ErrorResult
import models.errors.ErrorResult.leppSubmissionError
import models.requests.DataRequest
import models.userAnswers.{BankAccountDetails, LeppItem, LeppSummary, SubmissionSummary}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import utils.Constants.noCorrelationIdString
import utils.{Logging, MethodContext}

import scala.concurrent.ExecutionContext

@Singleton
class LeppSubmissionService @Inject()(connector: AcceptLeppPaymentConnector,
                                      auditService: AuditService) extends Logging {

  protected[services] def acceptPayment(request: AcceptLeppPaymentRequest, entitlement: BigDecimal)
                                       (using requestCid: CorrelationId)
                                       (using HeaderCarrier, ExecutionContext): ConnectorResponse[AcceptLeppPaymentResponse] = {
    given methodContext: MethodContext = MethodContext("acceptPayment")

    resultWithCid(connector.acceptPayment(request)).bimap(
      err =>
        logger.warn(
          msg = s"Failed to accept available payment for request with " +
            s"tax year: ${request.taxYear}, " +
            s"cid: ${err.correlationId}, " +
            s"error code ${err.value.code}, " +
            s"and error status ${err.value.status}"
        )
        auditService.auditSubmissionFailure(
          nino = request.identifier,
          bankAccountDetails = request.body.lowEarnersAccountDetails,
          taxYear = request.taxYear,
          entitlement = entitlement
        )
        err,
      success =>
        logger.info(
          msg = s"Successfully accepted available payment for request with" +
            s"tax year: ${request.taxYear}, " +
            s"and cid: ${success.correlationId}"
        )
        auditService.auditSubmissionSuccess(
          nino = request.identifier,
          bankAccountDetails = request.body.lowEarnersAccountDetails,
          taxYear = request.taxYear,
          entitlement = entitlement
        )
        success
    )
  }
  
  def acceptMultiplePayments[A](nino: Nino, leppSummary: LeppSummary, accountDetails: BankAccountDetails)
                               (using cid: CorrelationId)
                               (using HeaderCarrier, ExecutionContext): ConnectorResponse[SubmissionSummary] = {
    given methodContext: MethodContext = MethodContext("acceptMultiple")
    
    val toAccept: Seq[LeppItem] = leppSummary.availableItems.getOrElse(Nil)

    logger.info(s"Attempting to accept all ${toAccept.length} available payments for request with cid: $cid")

    val partialAcceptRequest: (BigInt, BigInt) => AcceptLeppPaymentRequest = AcceptLeppPaymentRequest(
      nino = nino,
      bankAccountDetails = accountDetails
    )
    
    def doSubmit(toAccept: Seq[LeppItem],
                 currentLock: BigInt,
                 submissionSummary: SubmissionSummary): ConnectorResponse[SubmissionSummary] = toAccept match {
      case head :: tail => 
        import head.{id, taxYear, entitlement}
        
        logger.info(msg = s"Attempting to accept available payment for request with taxYear: $taxYear, and cid: $cid")

        val acceptRequest: AcceptLeppPaymentRequest = partialAcceptRequest(currentLock, taxYear)
        val result: ConnectorResponse[AcceptLeppPaymentResponse] = acceptPayment(acceptRequest, entitlement)
        
        result.biflatMap(
          err =>
            logger.warn(
              msg = s"Failed to accept all available payments for request with cid: $cid returning submission summary"
            )
            tail.foreach(item =>
              auditService.auditSubmissionFailure(
                nino = nino,
                bankAccountDetails = accountDetails,
                taxYear = taxYear,
                entitlement = entitlement,
                wasSkipped = true
              )
            )
            if (submissionSummary.isEmpty) {
              rawConnectorFailure[SubmissionSummary](leppSubmissionError)
            } else {
              rawConnectorSuccess(submissionSummary.copy(notAcceptedIds = toAccept.map(_.id)))
            },
          success =>
            val updatedLock: BigInt = success.value.updatedLowEarnersOptimisticLock
            doSubmit(
              toAccept = tail,
              currentLock = updatedLock,
              submissionSummary = submissionSummary.addAccepted(id)
            )
        )
      case Nil =>
        logger.info(msg = s"No available payments to accept for request with cid: $cid. Returning submission summary")
        rawConnectorSuccess(submissionSummary)
    }
    
    doSubmit(
      toAccept = toAccept,
      currentLock = leppSummary.currentLock,
      submissionSummary = SubmissionSummary.empty
    )
  }
  
  protected[services] def resultWithCid(result: ConnectorResponse[AcceptLeppPaymentResponse])
                                       (using cid: CorrelationId)
                                       (using ExecutionContext): ConnectorResponse[AcceptLeppPaymentResponse] = {
    val resultCid = (responseCid: CorrelationId) => if (responseCid.value == noCorrelationIdString) cid else responseCid
    
    result.bimap(
      err => err.copy(correlationId = resultCid(err.correlationId)),
      succ => succ.copy(correlationId = resultCid(succ.correlationId)),
    )
  }
}
