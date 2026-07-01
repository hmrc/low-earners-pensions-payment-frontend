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

import play.api.Configuration
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import uk.gov.hmrc.play.bootstrap.config.AppName
import models.audit.*
import models.audit.PaymentOutcome.*
import models.userAnswers.{BankAccountDetails, LeppItem}
import uk.gov.hmrc.domain.Nino
import com.google.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuditService @Inject()(auditConnector: AuditConnector, appConfig: Configuration) {
  private val SUBMISSION_AUDIT_TYPE: String = "SubmitForPayment"
  private val SUBMISSION_TRANSACTION_NAME: String = "submit-for-payment"
  private val SUBMISSION_AUDIT_PATH: String = controllers.routes.CheckYourAnswersController.onSubmit().url
  
  def auditSubmissionSuccess(nino: Nino,
                             bankAccountDetails: BankAccountDetails,
                             taxYear: BigInt,
                             entitlement: BigDecimal)
                            (using HeaderCarrier, ExecutionContext): Unit = {
    val auditDetail: AuditDetail = AuditDetail(
      bankAccountDetails = bankAccountDetails,
      nino = nino,
      taxYear = taxYear,
      entitlement = entitlement,
      paymentOutcome = pass
    )

    val event: AuditEvent[AuditDetail] = AuditEvent(
      auditType = SUBMISSION_AUDIT_TYPE,
      transactionName = SUBMISSION_TRANSACTION_NAME,
      path = SUBMISSION_AUDIT_PATH,
      detail = auditDetail
    )

    auditEvent(event)
  }
  
  def auditSubmissionFailure(nino: Nino,
                             bankAccountDetails: BankAccountDetails,
                             taxYear: BigInt,
                             entitlement: BigDecimal,
                             wasSkipped: Boolean = false)
                            (using HeaderCarrier, ExecutionContext): Unit = {
    val paymentOutcome: PaymentOutcome = if (wasSkipped) skipped else fail

    val auditDetail: AuditDetail = AuditDetail(
      bankAccountDetails = bankAccountDetails,
      nino = nino,
      taxYear = taxYear,
      entitlement = entitlement,
      paymentOutcome = paymentOutcome
    )

    val event: AuditEvent[AuditDetail] = AuditEvent(
      auditType = SUBMISSION_AUDIT_TYPE,
      transactionName = SUBMISSION_TRANSACTION_NAME,
      path = SUBMISSION_AUDIT_PATH,
      detail = auditDetail
    )

    auditEvent(event)
  }

  private def auditEvent[T](event: AuditEvent[T])
                           (using hc: HeaderCarrier)
                           (using ExecutionContext, Writes[T]): Future[AuditResult] = {

    val eventTags = AuditExtensions.auditHeaderCarrier(hc).toAuditTags() ++
      Map("transactionName" -> event.transactionName, "path" -> event.path)

    val extendedDataEvent = ExtendedDataEvent(
      auditSource = AppName.fromConfiguration(appConfig),
      auditType = event.auditType,
      detail = Json.toJson(event.detail),
      tags = eventTags
    )

    auditConnector.sendExtendedEvent(extendedDataEvent)
  }
}
