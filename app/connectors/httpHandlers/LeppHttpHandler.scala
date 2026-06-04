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

import models.ResponseWrapper.{ErrorWrapper, HttpResponseWrapper}
import models.errors.ErrorResult
import models.errors.ErrorResult.ServiceErrorResult
import models.nps.RetrieveLeppDetailsResponse
import play.api.http.Status.*
import utils.ErrorCodes
import utils.ErrorCodes.{BAD_REQUEST_ERROR, INTERNAL_ERROR, NOT_FOUND_ERROR}

trait LeppHttpHandler extends HttpHandler[RetrieveLeppDetailsResponse]{

  override val errorMap: ErrorResult => ErrorResult = err => ServiceErrorResult(err.status, err.code)

  override def statusHandler(method: String, url: String, response: HttpResponseWrapper): HttpResult = {
    def errorResponse(status: Int, code: String): HttpResult = Left(ErrorWrapper(
      value = ServiceErrorResult(status, code),
      correlationId = response.correlationId
    ))

    response.value.status match {
      case OK => Right(response)
      case BAD_REQUEST => errorResponse(BAD_REQUEST, BAD_REQUEST_ERROR)
      case NOT_FOUND => errorResponse(NOT_FOUND, NOT_FOUND_ERROR)
      case _ => errorResponse(INTERNAL_SERVER_ERROR, INTERNAL_ERROR)
    }
  }
}
