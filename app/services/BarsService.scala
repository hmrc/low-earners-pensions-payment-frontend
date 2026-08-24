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
import models.bars.{BarsFailedCheckError, BarsRequest, BarsRequestError, BarsResponse}
import models.errors.ErrorResult.BarsErrorResult
import play.api.http.Status.*
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Logging, MethodContext}

import scala.concurrent.ExecutionContext

@Singleton
class BarsService @Inject()(connector: BarsConnector) extends Logging {
  def checkBankAccountDetails(barsRequest: BarsRequest)
                             (implicit hc: HeaderCarrier,
                              ec: ExecutionContext,
                              cid: CorrelationId): ConnectorResponse[BarsResponse] = {
    given mc: MethodContext = MethodContext("checkBankAccountDetails")

    connector
      .checkBankAccountDetails(request = barsRequest)
      .subflatMap(response => response.value.toBarsErrors match {
        case Nil =>
          //Any auditing here
          Right(response)
        case errors =>
          val errorResults: Seq[BarsErrorResult] = errors.map {
            case error: BarsFailedCheckError => BarsErrorResult(INTERNAL_SERVER_ERROR, error.reason)
            case error: BarsRequestError => BarsErrorResult(BAD_REQUEST, error.reason)
          }

          val errorReasons = errors.map(_.reason).mkString(",")

          val errorResult: BarsErrorResult = errorResults match {
            case errs if errs.exists(_.status == INTERNAL_SERVER_ERROR) =>
              logger.error(s"The request to BARS returned server failures, reasons: $errorReasons")
              BarsErrorResult(INTERNAL_SERVER_ERROR, "BARS_CHECK_FAILED", Some(errs))
            case errs =>
              logger.info(s"The request to BARS returned validation errors, reasons: $errorReasons")
              BarsErrorResult(BAD_REQUEST, "BARS_REQUEST_ERRORS", Some(errs))
          }

          //Any auditing here
          Left(ErrorWrapper(value = errorResult, correlationId = response.correlationId))
      })
  }
}
