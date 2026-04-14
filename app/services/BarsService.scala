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
import connectors.{BarsConnector, ConnectorResponse}
import models.CorrelationId
import models.ResponseWrapper.ErrorWrapper
import models.bars.{BarsRequest, BarsResponse}
import models.errors.ErrorResult.BarsErrorResult
import play.api.http.Status.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

@Singleton
class BarsService @Inject()(connector: BarsConnector) {
  def checkBankAccountDetails(barsRequest: BarsRequest, correlationId: CorrelationId)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): ConnectorResponse[BarsResponse] = {
    connector
      .checkBankAccountDetails(request = barsRequest, correlationId = correlationId)
      .subflatMap(response => response.value.toErrorResultOpt match {
        case None =>
          //Any auditing here
          Right(response)
        case Some(error) =>
          //Any auditing here
          Left(ErrorWrapper(
            value = BarsErrorResult(BAD_REQUEST, "BARS_RESPONSE_ERROR"),
            correlationId = response.correlationId
          ))
      })
  }
}
