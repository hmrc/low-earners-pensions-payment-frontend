package connectors

import cats.data.EitherT
import com.google.inject.Singleton
import models.CorrelationId
import models.ResponseWrapper.{ErrorWrapper, SuccessWrapper}
import models.backend.{SubmitClaimRequest, SubmitClaimResponse}

import scala.concurrent.Future

@Singleton
class PlaceholderBackendConnector {
  def submitClaim(request: SubmitClaimRequest): EitherT[Future, ErrorWrapper, SuccessWrapper[SubmitClaimResponse]] =
    EitherT(Future.successful(Right(SuccessWrapper(SubmitClaimResponse(1), CorrelationId("N/A")))))
}
