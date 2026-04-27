package services

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import connectors.{ConnectorResponse, PlaceholderBackendConnector}
import models.CorrelationId
import models.ResponseWrapper.SuccessWrapper
import models.backend.{SubmitClaimRequest, SubmitClaimResponse, SubmitClaimResponseSuccess, SubmitClaimResponseWithFailures}
import models.userAnswers.{BankAccountDetails, ClaimItem, ClaimsSummary}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimSubmissionService @Inject()(placeholderBackendConnector: PlaceholderBackendConnector) {
  protected[services] def submitSingleClaim(currentLock: Int,
                                            taxYear: Int,
                                            bankAccountDetails: BankAccountDetails)
                                           (implicit hc: HeaderCarrier,
                                            ec: ExecutionContext,
                                            cid: CorrelationId): ConnectorResponse[SubmitClaimResponse] = {
    placeholderBackendConnector.submitClaim(
      SubmitClaimRequest(
        currentLowEarnersOptimisticLock = currentLock,
        taxYear = taxYear,
        accountDetails = bankAccountDetails
      )
    )
  }
  
  
  
  def submitClaims(claimsSummary: ClaimsSummary, bankAccountDetails: BankAccountDetails)
                  (implicit hc: HeaderCarrier,
                   ec: ExecutionContext,
                   cid: CorrelationId): ConnectorResponse[SubmitClaimResponse] = {

    def doClaim(currentLock: Int, claims: Seq[ClaimItem], hasFailures: Boolean = false)
               (implicit hc: HeaderCarrier,
                ec: ExecutionContext,
                cid: CorrelationId): ConnectorResponse[SubmitClaimResponse] = {
      claims match {
        case Nil => EitherT(
          Future.successful(Right(SuccessWrapper(
            value = if (hasFailures) {
              SubmitClaimResponseWithFailures(currentLock)
            } else {
              SubmitClaimResponseSuccess(currentLock)
            },
            correlationId = cid
          )))
        )
        case nextClaim +: claims =>
          submitSingleClaim(currentLock, nextClaim.taxYear, bankAccountDetails).biflatMap(
            err =>
              implicit val cid: CorrelationId = err.correlationId
              // Iff a claim fails we skip to the next one + some logging here
              doClaim(currentLock = currentLock, claims = claims, hasFailures = true),
            succ =>
              implicit val cid: CorrelationId = succ.correlationId
              doClaim(succ.value.updatedLowEarnersOptimisticLock, claims)
          )
      }
    }

    doClaim(claimsSummary.currentLock, claimsSummary.claims)
  }
}
