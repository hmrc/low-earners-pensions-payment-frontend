package connectors

import cats.data.EitherT
import com.google.inject.Singleton
import models.CorrelationId
import models.ResponseWrapper.{ErrorWrapper, SuccessWrapper}
import models.backend.{SubmitClaimRequest, SubmitClaimResponse, SubmitClaimResponseSuccess}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PlaceholderBackendConnector {
  def submitClaim(request: SubmitClaimRequest)
                 (implicit hc: HeaderCarrier,
                  ec: ExecutionContext,
                  cid: CorrelationId): ConnectorResponse[SubmitClaimResponse] =
    EitherT(Future.successful(Right(SuccessWrapper(SubmitClaimResponseSuccess(1), CorrelationId("N/A")))))
}
