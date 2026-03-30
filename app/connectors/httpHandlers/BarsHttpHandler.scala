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

package connectors.httpHandlers

import models.CorrelationId
import models.ResponseWrapper.{ErrorWrapper, HttpResponseWrapper}
import models.bars.BarsResponse
import models.errors.ErrorResult
import models.errors.ErrorResult.BarsErrorResult
import play.api.http.Status.*
import uk.gov.hmrc.http.HttpResponse

trait BarsHttpHandler extends HttpHandler[BarsResponse]{
  override def correlationIdHandler[A](httpResponse: HttpResponse): HttpResult =
    Right(HttpResponseWrapper(httpResponse, CorrelationId(noCorrelationIdString)))
  
  override val errorMap: ErrorResult => ErrorResult = err => BarsErrorResult(err.status, err.code)

  override def statusHandler(method: String, url: String, response: HttpResponseWrapper): HttpResult = {
    def errorResponse(code: String): HttpResult = Left(ErrorWrapper(
      value = BarsErrorResult(INTERNAL_SERVER_ERROR, code),
      correlationId = response.correlationId
    ))

    response.value.status match {
      case OK => Right(response)
      //TODO - As of right now the redirects are just followed by default so this line does nothing
      case MOVED_PERMANENTLY | SEE_OTHER | TEMPORARY_REDIRECT => errorResponse("BARS_RETURNED_REDIRECT")
      case BAD_REQUEST => errorResponse("ERROR_IN_BARS_REQUEST")
      case FORBIDDEN | NOT_FOUND => errorResponse("COULD_NOT_ACCESS_BARS_RESOURCE")
      case INTERNAL_SERVER_ERROR => errorResponse("BARS_INTERNAL_SERVER_ERROR")
      case _ => errorResponse("BARS_RETURNED_UNEXPECTED_STATUS")
    }
  }
}
