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
import play.api.http.Status.*
import play.api.libs.json.Reads
import utils.ErrorCodes
import utils.ErrorCodes.{BAD_REQUEST_ERROR, INTERNAL_ERROR, NOT_FOUND_ERROR, UNEXPECTED_STATUS}

trait LeppHttpHandler[Resp: Reads] extends HttpHandler[Resp]{
  val successStatus: Int = OK
  override val errorMap: ErrorResult => ErrorResult = err => ServiceErrorResult(err.status, err.code)

  val errorStatusMap: Map[Int, String]
  
  override def statusHandler(method: String, url: String, response: HttpResponseWrapper): HttpResult = {
    def errorResponse(status: Int, code: String): HttpResult = Left(ErrorWrapper(
      value = ServiceErrorResult(status, code),
      correlationId = response.correlationId
    ))

    response.value.status match {
      case `successStatus` => Right(response)
      case errorStatus => errorStatusMap.get(errorStatus) match {
        case Some(errorCode) => errorResponse(errorStatus, errorCode)
        case _ => errorResponse(INTERNAL_SERVER_ERROR, UNEXPECTED_STATUS)
      }
    }
  }
}
