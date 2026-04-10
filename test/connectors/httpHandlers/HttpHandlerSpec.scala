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

import base.SpecBase
import connectors.DownstreamResponse
import models.ResponseWrapper.{ErrorWrapper, HttpResponseWrapper, SuccessWrapper}
import models.errors.ErrorResult
import models.errors.ErrorResult.{ServiceErrorResult, failedToParseError}
import models.{CorrelationId, ResponseWrapper}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.HttpResponse

class HttpHandlerSpec extends SpecBase {

  private case class TestClass(value: String)

  private object TestClass {
    implicit val format: OFormat[TestClass] = Json.format[TestClass]
  }

  private object TestObject extends HttpHandler[TestClass] {
    override def correlationIdHandler[A](httpResponse: HttpResponse): HttpResult =
      httpResponse.header("correlationId") match {
        case Some(value) => Right(HttpResponseWrapper(httpResponse, CorrelationId(value)))
        case None => Left(ErrorWrapper(ServiceErrorResult(BAD_REQUEST, "BAD_REQUEST"), CorrelationId("N/A")))
      }

    override def statusHandler(method: String,
                               url: String,
                               response: ResponseWrapper.HttpResponseWrapper): HttpResult =
      response.value.status match {
        case OK => Right(response)
        case _ => Left(ErrorWrapper(ServiceErrorResult(IM_A_TEAPOT, "TEAPOT_TIME"), response.correlationId))
      }
  }

  "HttpHandler" - {
    "validateBody" - {
      def resultCreator(body: String) = TestObject.validateBody(
        method = "POST",
        url = "url",
        response = HttpResponseWrapper(HttpResponse(IM_A_TEAPOT, body), testCorrelationId)
      )

      "when mandatory fields are missing should return an error" in {
        val result: DownstreamResponse[TestClass] = resultCreator("")
        val expectedError = ErrorWrapper(
          value = ServiceErrorResult(INTERNAL_SERVER_ERROR, "FAILED_TO_PARSE_DOWNSTREAM_RESPONSE"),
          correlationId = testCorrelationId
        )

        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorWrapper) mustBe expectedError
      }

      "when JSON is invalid should return an error" in {
        val result: DownstreamResponse[TestClass] = resultCreator("""{"field"}""")
        val expectedError = ErrorWrapper(
          value = ServiceErrorResult(INTERNAL_SERVER_ERROR, "FAILED_TO_PARSE_DOWNSTREAM_RESPONSE"),
          correlationId = testCorrelationId
        )

        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorWrapper) mustBe expectedError
      }

      "when a field has an incorrect data type should return an error" in {
        val result: DownstreamResponse[TestClass] = resultCreator("""{"value": 2}""")
        val expectedError = ErrorWrapper(
          value = ServiceErrorResult(INTERNAL_SERVER_ERROR, "FAILED_TO_PARSE_DOWNSTREAM_RESPONSE"),
          correlationId = testCorrelationId
        )

        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorWrapper) mustBe expectedError
      }

      "should return the expected data model when JSON is valid" in {
        val result: DownstreamResponse[TestClass] = resultCreator(
          """
            |{
            | "value": "some-value"
            |}
        """.stripMargin)
        result mustBe a[Right[_, _]]
        val expectedResult = SuccessWrapper(TestClass("some-value"), testCorrelationId)
        result.getOrElse(SuccessWrapper(TestClass("N/A"), testCorrelationId)) mustBe expectedResult
      }
    }

    "httpReads" - {
      "should return an error when correlationIdHandler returns an error" in {
        val result: DownstreamResponse[TestClass] = TestObject.httpReads.read("N/A", "N/A", HttpResponse(OK, ""))
        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorWrapper).value mustBe ServiceErrorResult(BAD_REQUEST, "BAD_REQUEST")
      }

      "should return an error when statusHandler returns an error" in {
        val result: DownstreamResponse[TestClass] = TestObject.httpReads.read(
          method = "N/A",
          url = "N/A",
          response = HttpResponse(BAD_REQUEST, "", Map("correlationId" -> Seq("id")))
        )

        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorWrapper).value mustBe ServiceErrorResult(IM_A_TEAPOT, "TEAPOT_TIME")
      }

      "should return an error when bodyValidation returns an error" in {
        val result: DownstreamResponse[TestClass] = TestObject.httpReads.read(
          method = "N/A",
          url = "N/A",
          response = HttpResponse(OK, "", Map("correlationId" -> Seq("id")))
        )

        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorWrapper).value mustBe failedToParseError
      }

      "should return a success response when checks pass" in {
        val result: DownstreamResponse[TestClass] = TestObject.httpReads.read(
          method = "N/A",
          url = "N/A",
          response = HttpResponse(
            status = OK,
            body =
              """
                |{
                | "value": "value"
                |}
                |""".stripMargin,
            headers = Map("correlationId" -> Seq(testCorrelationId.value)))
        )

        result mustBe a[Right[_, _]]
        val expectedResult: ResponseWrapper[TestClass] = SuccessWrapper(TestClass("value"), testCorrelationId)
        result.getOrElse(SuccessWrapper(TestClass("N/A"), CorrelationId("N/A"))) mustBe expectedResult
      }
    }
  }
}
