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
import models.backend.*
import models.backend.retrieve.RetrieveLeppDetailsResponse
import models.errors.ErrorResult.BackendErrorResult
import models.{CorrelationId, ResponseWrapper}
import play.api.http.Status.*
import uk.gov.hmrc.http.HttpResponse
import utils.ErrorCodes.{BAD_REQUEST_ERROR, INTERNAL_ERROR, NOT_FOUND_ERROR}

class LeppHttpHandlerSpec extends SpecBase {

  private object TestObject extends LeppHttpHandler[RetrieveLeppDetailsResponse] {
    override val errorStatusMap: Map[Int, String] = Map(
      BAD_REQUEST -> BAD_REQUEST_ERROR,
      NOT_FOUND -> NOT_FOUND_ERROR,
      INTERNAL_SERVER_ERROR -> INTERNAL_ERROR
    )
  }

  "LeppHttpHandler" - {
    "statusHandler" - {
      "should return a success for a 200 status code" in {
        val httpResponse: HttpResponse = HttpResponse(OK, "")

        val result: TestObject.HttpResult = TestObject.statusHandler(
          method = "HttpMethod",
          url = "url",
          response = HttpResponseWrapper(httpResponse, testCorrelationId)
        )

        result mustBe a[Right[_, _]]
        result.getOrElse(leppResponse).value mustBe httpResponse
      }

      def handleForErrorScenario(status: Int, code: String): Unit =
        s"for response with status - $status should return error with code - $code" in {
          val result: TestObject.HttpResult = TestObject.statusHandler(
            method = "aMethod",
            url = "aUrl",
            response = HttpResponseWrapper(HttpResponse(status, ""), testCorrelationId)
          )

          result mustBe a[Left[_, _]]
          val expectedResult = ErrorWrapper(BackendErrorResult(status, code), testCorrelationId)
          result.swap.getOrElse(dummyErrorWrapper) mustBe expectedResult
        }

      val errorScenarios: Seq[(Int, String)] = Seq(
        (BAD_REQUEST, BAD_REQUEST_ERROR),
        (NOT_FOUND, NOT_FOUND_ERROR),
        (INTERNAL_SERVER_ERROR, INTERNAL_ERROR)
      )

      errorScenarios.foreach((status, code) => handleForErrorScenario(status, code))
    }
    
    "httpReads" - {
      "should return an error for a handled error status" in {
        val result: DownstreamResponse[RetrieveLeppDetailsResponse] = TestObject.httpReads.read(
          method = "aMethod",
          url = "aUrl",
          response = HttpResponse(BAD_REQUEST, "")
        )
        
        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorWrapper).value mustBe
          BackendErrorResult(BAD_REQUEST, BAD_REQUEST_ERROR)
      }

      "should return an error for an unhandled error status" in {
        val result: DownstreamResponse[RetrieveLeppDetailsResponse] = TestObject.httpReads.read(
          method = "aMethod",
          url = "aUrl",
          response = HttpResponse(IM_A_TEAPOT, "")
        )

        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorWrapper).value mustBe
          BackendErrorResult(INTERNAL_SERVER_ERROR, "UNEXPECTED_STATUS")
      }
      
      "should return an error for an invalid response body" in {
        val result: DownstreamResponse[RetrieveLeppDetailsResponse] = TestObject.httpReads.read(
          method = "aMethod",
          url = "aUrl",
          response = HttpResponse(OK, "")
        )
        
        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorWrapper).value mustBe
          BackendErrorResult(INTERNAL_SERVER_ERROR, "FAILED_TO_PARSE_DOWNSTREAM_RESPONSE")
      }


      "should return a success for a valid response" in {
        val retrieveJson: String =
          """
            |{
            | "currentLowEarnersOptimisticLock": 123,
            | "identifier": "id",
            | "lowEarnersDetailsList": [
            |   {
            |     "taxYear": 11,
            |     "lowEarnersCalculations": [
            |       {
            |         "lowEarnersClaimDetails": {
            |           "claimSequenceNumber": 123,
            |           "calculationDate": "2023-06-27",
            |           "claimDate": "2023-06-27",
            |           "claimStatus": "PAID",
            |           "entitlementAmount": 10.56,
            |           "inSelfAssessment": true,
            |           "originalAmount": 10.56,
            |           "reissueClaimOutput": true,
            |           "reminderOutputSent": true
            |         },
            |         "lowEarnersDataDetails": {
            |           "calculationSequenceNumber": 123,
            |           "basicRatePercentage": 0.23,
            |           "dataSourceMaster": "CESA",
            |           "netPayContributionsTotal": 10.56,
            |           "responseTimestamp": "2023-06-27 09:12:28",
            |           "totalAllowances": 10.56,
            |           "totalDeductions": 10.56,
            |           "totalIncome": 10.56,
            |           "totalTaxDue": 10.56
            |         }
            |       }
            |     ]
            |   }
            | ]
            |}
    """.stripMargin

        val result: DownstreamResponse[RetrieveLeppDetailsResponse] = TestObject.httpReads.read(
          method = "aMethod",
          url = "aUrl",
          response = HttpResponse(OK, retrieveJson)
        )

        result mustBe a[Right[_, _]]

        val expectedRetrieveLeppDetailsResponse: RetrieveLeppDetailsResponse = retrieveResponse

        result.getOrElse(leppResponse) mustBe
          SuccessWrapper(expectedRetrieveLeppDetailsResponse, CorrelationId("NO_CORRELATION_ID_IN_RESPONSE"))
      }
    }
  }

}
