/*
 * Copyright 2024 HM Revenue & Customs
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

package connectors

import cats.data.EitherT
import com.google.inject.Inject
import config.AppConfig
import connectors.httpHandlers.{HttpHandler, LeppHttpHandler}
import models.CorrelationId
import models.backend.retrieve.RetrieveLeppDetailsResponse
import play.api.http.Status.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import utils.Constants.correlationIdKey
import utils.ErrorCodes.{BAD_REQUEST_ERROR, INTERNAL_ERROR, NOT_FOUND_ERROR}
import utils.{Logging, MethodContext}
import scala.language.implicitConversions

import scala.concurrent.ExecutionContext

class LeppRetrievalConnector @Inject()(config: AppConfig, httpClient: HttpClientV2)
  extends LeppHttpHandler[RetrieveLeppDetailsResponse] with Logging {

  def retrieveLeppDetails()(implicit hc: HeaderCarrier,
                                      ec: ExecutionContext,
                                      correlationId: CorrelationId): ConnectorResponse[RetrieveLeppDetailsResponse] = {
    val getPaymentDetailsUrl = url"${config.getPaymentsUrl}"
    given methodLoggingContext: MethodContext = MethodContext("retrieveLeppDetails")

    logger.info(s"Calling NPS for the payment details with correlationId - $correlationId")

    EitherT(
      httpClient
        .get(getPaymentDetailsUrl)
        .setHeader((correlationIdKey, correlationId))
        .execute[DownstreamResponse[RetrieveLeppDetailsResponse]]
    )
  }

  override val errorStatusMap: Map[Int, String] = Map(
    BAD_REQUEST -> BAD_REQUEST_ERROR,
    NOT_FOUND -> NOT_FOUND_ERROR,
    INTERNAL_SERVER_ERROR -> INTERNAL_ERROR,
  )
}
