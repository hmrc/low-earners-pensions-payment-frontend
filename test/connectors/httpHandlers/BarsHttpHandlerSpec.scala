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
import models.bars.BarsResponse
import models.bars.statuses.*
import models.errors.ErrorResult.{BarsErrorResult, ServiceErrorResult}
import models.{CorrelationId, ResponseWrapper}
import play.api.http.Status.*
import uk.gov.hmrc.http.HttpResponse

class BarsHttpHandlerSpec extends SpecBase {

  private object TestObject extends BarsHttpHandler

  "BarsHttpHandler" - {
    "correlationIdHandler" - {
      "should handle with placeholder Correlation ID" in {
        val httpResponse: HttpResponse = HttpResponse(OK, "")
        val result = TestObject.correlationIdHandler(httpResponse)
        result mustBe a[Right[_, _]]
        val dummyResponse: ResponseWrapper[HttpResponse] = HttpResponseWrapper(HttpResponse(IM_A_TEAPOT, "N/A"), CorrelationId("N/A"))
        result.getOrElse(dummyResponse) mustBe HttpResponseWrapper(httpResponse, CorrelationId("NO_CORRELATION_ID_IN_RESPONSE"))
      }
    }


    "errorMap" - {
      "should map a generic service error to a BARS error" in {
        TestObject.errorMap(ServiceErrorResult(IM_A_TEAPOT, "TEAPOT_TIME")) mustBe BarsErrorResult(IM_A_TEAPOT, "TEAPOT_TIME")
      }
    }

    "statusHandler" - {
      "should return a success for a 200 status code" in {
        val httpResponse: HttpResponse = HttpResponse(OK, "")

        val result: TestObject.HttpResult = TestObject.statusHandler(
          method = "aMethod",
          url = "aUrl",
          response = HttpResponseWrapper(httpResponse, testCorrelationId)
        )

        result mustBe a[Right[_, _]]
        result.getOrElse(dummySuccessResponse).value mustBe httpResponse
      }

      def handleForErrorScenario(status: Int, code: String): Unit =
        s"for response with status - $status should return error with code - $code" in {
          val result: TestObject.HttpResult = TestObject.statusHandler(
            method = "aMethod",
            url = "aUrl",
            response = HttpResponseWrapper(HttpResponse(status, ""), testCorrelationId)
          )

          result mustBe a[Left[_, _]]
          val expectedResult = ErrorWrapper(BarsErrorResult(INTERNAL_SERVER_ERROR, code), testCorrelationId)
          result.swap.getOrElse(dummyErrorWrapper) mustBe expectedResult
        }

      val errorScenarios: Seq[(Int, String)] = Seq(
        (MOVED_PERMANENTLY, "BARS_RETURNED_REDIRECT"),
        (SEE_OTHER, "BARS_RETURNED_REDIRECT"),
        (TEMPORARY_REDIRECT, "BARS_RETURNED_REDIRECT"),
        (BAD_REQUEST, "ERROR_IN_BARS_REQUEST"),
        (FORBIDDEN, "COULD_NOT_ACCESS_BARS_RESOURCE"),
        (NOT_FOUND, "COULD_NOT_ACCESS_BARS_RESOURCE"),
        (INTERNAL_SERVER_ERROR, "BARS_INTERNAL_SERVER_ERROR"),
        (IM_A_TEAPOT, "BARS_RETURNED_UNEXPECTED_STATUS")
      )

      errorScenarios.foreach((status, code) => handleForErrorScenario(status, code))
    }
    
    "httpReads" - {
      "should return an error for a handled error status" in {
        val result: DownstreamResponse[BarsResponse] = TestObject.httpReads.read(
          method = "aMethod",
          url = "aUrl",
          response = HttpResponse(BAD_REQUEST, "")
        )
        
        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorWrapper).value mustBe
          BarsErrorResult(INTERNAL_SERVER_ERROR, "ERROR_IN_BARS_REQUEST")
      }

      "should return an error for an unhandled error status" in {
        val result: DownstreamResponse[BarsResponse] = TestObject.httpReads.read(
          method = "aMethod",
          url = "aUrl",
          response = HttpResponse(IM_A_TEAPOT, "")
        )

        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorWrapper).value mustBe
          BarsErrorResult(INTERNAL_SERVER_ERROR, "BARS_RETURNED_UNEXPECTED_STATUS")
      }
      
      "should return an error for an invalid response body" in {
        val result: DownstreamResponse[BarsResponse] = TestObject.httpReads.read(
          method = "aMethod",
          url = "aUrl",
          response = HttpResponse(OK, "")
        )
        
        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorWrapper).value mustBe
          BarsErrorResult(INTERNAL_SERVER_ERROR, "FAILED_TO_PARSE_DOWNSTREAM_RESPONSE")
      }


      "should return a success for a valid response" in {
        val jsonString: String =
          """
            |{
            | "nonStandardAccountDetailsRequiredForBacs": "no",
            | "sortCodeSupportsDirectDebit": "yes",
            | "sortCodeSupportsDirectCredit": "yes",
            | "accountNumberIsWellFormatted": "indeterminate",
            | "nameMatches": "indeterminate",
            | "sortCodeIsPresentOnEISCD": "yes",
            | "sortCodeBankName": "Test",
            | "accountExists": "no",
            | "accountName": "Taxwell Payer",
            | "iban": "test-iban"
            |}
          """.stripMargin

        val result: DownstreamResponse[BarsResponse] = TestObject.httpReads.read(
          method = "aMethod",
          url = "aUrl",
          response = HttpResponse(OK, jsonString)
        )

        result mustBe a[Right[_, _]]

        val expectedBarsResponse: BarsResponse = BarsResponse(
          accountNumberIsWellFormatted = AccountNumberWellFormatted.Indeterminate,
          accountExists = AccountExists.No,
          nameMatches = NameMatches.Indeterminate,
          accountName = Some("Taxwell Payer"),
          nonStandardAccountDetailsRequiredForBacs = NonStandardAccountDetails.No,
          sortCodeIsPresentOnEISCD = SortCodeCheck.Yes,
          sortCodeSupportsDirectDebit = SortCodeCheck.Yes,
          sortCodeSupportsDirectCredit = SortCodeCheck.Yes,
          sortCodeBankName = Some("Test"),
          iban = Some("test-iban")
        )

        result.getOrElse(dummySuccessResponse) mustBe
          SuccessWrapper(expectedBarsResponse, CorrelationId("NO_CORRELATION_ID_IN_RESPONSE"))
      }
    }
  }

}
