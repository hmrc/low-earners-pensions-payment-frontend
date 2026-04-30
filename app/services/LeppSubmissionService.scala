package services

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import connectors.{ConnectorResponse, PlaceholderBackendConnector}
import models.CorrelationId
import models.ResponseWrapper.SuccessWrapper
import models.backend.{SubmitLeppRequest, SubmitLeppResponse}
import models.userAnswers.LeppItemStatus.Available
import models.userAnswers.{BankAccountDetails, LeppItem, LeppSummary}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LeppSubmissionService @Inject()(placeholderBackendConnector: PlaceholderBackendConnector) {
  protected[services] def submitSingle(currentLeppLock: Int, taxYear: Int, bankDetails: BankAccountDetails)
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

    def doSubmit(currentLeppLock: Int, toSubmit: Seq[LeppItem])
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

    doSubmit(leppSummary.currentLock, leppSummary.items.filter(_.status == Available))
  }
}
