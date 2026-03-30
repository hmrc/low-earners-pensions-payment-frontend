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
import models.bars.{BarsResponse, ValidatedBarsRequest}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

@Singleton
class BarsService @Inject()(connector: BarsConnector) {
  def checkBankAccountDetails(barsRequest: ValidatedBarsRequest, correlationId: CorrelationId)
                             (implicit hc: HeaderCarrier, ec: ExecutionContext): ConnectorResponse[BarsResponse] = {
    // If we want to do any mapping/ auditing of the BARS request I imagine we will do it here
    connector.checkBankAccountDetails(barsRequest, correlationId)
  }
}
