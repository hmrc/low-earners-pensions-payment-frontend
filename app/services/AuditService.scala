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
import AuditService.*

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuditService @Inject()(auditConnector: AuditConnector, appConfig: Configuration) {
  
  def auditSubmissionSuccess(nino: Nino, bankAccountDetails: BankAccountDetails, leppItem: LeppItem)
                            (using HeaderCarrier, ExecutionContext): Unit = {
    val auditDetail: AuditDetail = AuditDetail(
      bankAccountDetails = bankAccountDetails,
      nino = nino,
      leppItem = leppItem,
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
                             leppItem: LeppItem,
                             wasSkipped: Boolean = false)
                            (using HeaderCarrier, ExecutionContext): Unit = {
    val paymentOutcome: PaymentOutcome = if (wasSkipped) skipped else fail

    val auditDetail: AuditDetail = AuditDetail(
      bankAccountDetails = bankAccountDetails,
      nino = nino,
      leppItem = leppItem,
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

object AuditService {
  val SUBMISSION_AUDIT_TYPE: String = "SubmitForPayment"
  val SUBMISSION_TRANSACTION_NAME: String = "submit-for-payment"
  val SUBMISSION_AUDIT_PATH: String = controllers.routes.CheckYourAnswersController.onSubmit().url
}
