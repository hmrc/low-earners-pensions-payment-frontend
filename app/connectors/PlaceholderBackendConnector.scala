package connectors

import cats.data.EitherT
import com.google.inject.Singleton
import models.CorrelationId
import models.ResponseWrapper.{ErrorWrapper, SuccessWrapper}
import models.backend.{SubmitLeppRequest, SubmitLeppResponse}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PlaceholderBackendConnector {
  def submitLepp(request: SubmitLeppRequest)
                (implicit hc: HeaderCarrier,
                  ec: ExecutionContext,
                  cid: CorrelationId): ConnectorResponse[SubmitLeppResponse] =
    EitherT(Future.successful(Right(SuccessWrapper(SubmitLeppResponse(1), CorrelationId("N/A")))))
}
