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

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import connectors.DownstreamResponse
import models.ResponseWrapper.{ErrorWrapper, HttpResponseWrapper, SuccessWrapper}
import models.errors.ErrorResult
import models.errors.ErrorResult.failedToParseError
import models.{CorrelationId, ResponseWrapper}
import play.api.libs.json.*
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.Constants

trait HttpHandler[Resp: Reads] {
  type HttpResult = Either[ErrorWrapper, HttpResponseWrapper]

  def statusHandler(method: String, url: String, response: HttpResponseWrapper): HttpResult

  private val noCorrelationIdString: String = "NO_CORRELATION_ID_IN_RESPONSE"

  def correlationIdHandler[A](httpResponse: HttpResponse): HttpResult = {
    val correlationId: Option[String] = httpResponse.header(Constants.correlationIdKey)
    Right(HttpResponseWrapper(httpResponse, CorrelationId(correlationId.getOrElse(noCorrelationIdString))))
  }

  val errorMap: ErrorResult => ErrorResult = err => err

  protected[httpHandlers] def validateBody(method: String,
                                           url: String,
                                           response: HttpResponseWrapper): DownstreamResponse[Resp] = {
    val correlationId: CorrelationId = response.correlationId

    try {
      val responseJson: JsValue = Json.parse(response.value.body)
      responseJson.validate[Resp] match {
        case JsSuccess(value, _) =>
          Right[ErrorWrapper, SuccessWrapper[Resp]](SuccessWrapper(value, correlationId)).withLeft
        case JsError(errors) =>
          Left(ErrorWrapper(failedToParseError, correlationId))
      }
    } catch {
      case ex: JsonParseException =>
        Left(ErrorWrapper(failedToParseError, correlationId))
      case ex: JsonMappingException =>
        Left(ErrorWrapper(failedToParseError, correlationId))
    }
  }

  implicit val httpReads: HttpReads[DownstreamResponse[Resp]] = (method: String, url: String, response: HttpResponse) => {
    val result: Either[ErrorWrapper, ResponseWrapper.SuccessWrapper[Resp]] = for {
      correlationIdResult <- correlationIdHandler(response)
      statusResult <- statusHandler(method, url, correlationIdResult)
      bodyResult <- validateBody(method, url, statusResult)
    } yield bodyResult

    result match {
      case Left(err) => Left(ErrorWrapper(errorMap(err.value), err.correlationId))
      case Right(success) => Right(success)
    }
  }
}
