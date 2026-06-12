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
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import utils.Constants.correlationIdKey
import utils.Logging

import scala.concurrent.ExecutionContext

class LeppRetrievalConnector @Inject()(config: AppConfig, httpClient: HttpClientV2) extends LeppHttpHandler with Logging {

  def retrieveLeppDetails()(implicit hc: HeaderCarrier,
                                      ec: ExecutionContext,
                                      correlationId: CorrelationId): ConnectorResponse[RetrieveLeppDetailsResponse] = {
    val getPaymentDetailsUrl = url"${config.getPaymentsUrl}"
    val methodLoggingContext: String = "retrieveLeppDetails"

    logger.info(methodLoggingContext, s"Calling NPS for the payment details with correlationId - $correlationId")

    EitherT(
      httpClient
        .get(getPaymentDetailsUrl)
        .setHeader((correlationIdKey, correlationId))
        .execute[DownstreamResponse[RetrieveLeppDetailsResponse]]
    )
  }
}
