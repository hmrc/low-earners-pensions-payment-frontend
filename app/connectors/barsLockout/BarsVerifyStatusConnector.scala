/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors.barsLockout

import com.google.inject.Inject
import config.AppConfig
import connectors.barsLockout.model.{BarVerifyStatusId, BarsUpdateVerifyStatusParams, BarsVerifyStatusResponse}
import models.CorrelationId
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import utils.Constants.correlationIdKey

import scala.concurrent.{ExecutionContext, Future}

class BarsVerifyStatusConnector @Inject()(httpClient: HttpClientV2,
                                config: AppConfig) {
  
  def status(id: BarVerifyStatusId)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[BarsVerifyStatusResponse] =
    httpClient
      .post(url"${config.verifyStatus}")
      .setHeader((correlationIdKey, "correlationId"))
      .withBody(Json.toJson(BarsUpdateVerifyStatusParams(id)))
      .execute[BarsVerifyStatusResponse]

  def update(id: BarVerifyStatusId)(implicit hc: HeaderCarrier, ec: ExecutionContext,
                                    correlationId: CorrelationId): Future[BarsVerifyStatusResponse] =
    httpClient
      .post(url"${config.updateStatus}")
      .setHeader((correlationIdKey, correlationId.value))
      .withBody(Json.toJson(BarsUpdateVerifyStatusParams(id)))
      .execute[BarsVerifyStatusResponse]

}
