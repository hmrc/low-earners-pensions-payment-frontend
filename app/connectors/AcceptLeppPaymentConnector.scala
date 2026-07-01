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

package connectors

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import config.AppConfig
import connectors.httpHandlers.LeppHttpHandler
import models.CorrelationId
import models.backend.accept.{AcceptLeppPaymentRequest, AcceptLeppPaymentResponse}
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import utils.Constants.correlationIdKey
import utils.ErrorCodes.{BAD_REQUEST_ERROR, CONFLICT_ERROR, INTERNAL_ERROR}
import utils.Logging
import scala.language.implicitConversions

import java.net.URL
import scala.concurrent.ExecutionContext

@Singleton
class AcceptLeppPaymentConnector @Inject()(config: AppConfig, httpClient: HttpClientV2) 
  extends LeppHttpHandler[AcceptLeppPaymentResponse] with Logging {
  
  override val successStatus: Int = CREATED
  
  def acceptPayment(request: AcceptLeppPaymentRequest)
                   (implicit hc: HeaderCarrier, 
                    ec: ExecutionContext,
                    cid: CorrelationId): ConnectorResponse[AcceptLeppPaymentResponse] = {
    val acceptPaymentUrl: URL = url"${config.acceptPaymentUrl}/${request.taxYear}"
    
    EitherT(
      httpClient
        .post(acceptPaymentUrl)
        .withBody(Json.toJson(request.body))
        .setHeader((correlationIdKey, cid))
        .execute[DownstreamResponse[AcceptLeppPaymentResponse]]
    )
  }

  override val errorStatusMap: Map[Int, String] = Map(
    BAD_REQUEST -> BAD_REQUEST_ERROR,
    CONFLICT -> CONFLICT_ERROR,
    INTERNAL_SERVER_ERROR -> INTERNAL_ERROR,
  )
}
