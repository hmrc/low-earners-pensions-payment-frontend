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

package controllers

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import common.IntegrationSpecBase
import models.bars.{BarsAccount, BarsSubject, ValidatedBarsRequest}
import play.api.Application
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{writeableOf_AnyContentAsEmpty, route as routeRequest, running as runningRequest}
import play.mvc.Http.Status
import play.test.Helpers.*

import scala.concurrent.Future

class BarsControllerIntegrationSpec extends IntegrationSpecBase {

  "/bank-account-details" should {
    val defaultParams: Map[String, String] = Map(
      "name" -> "aName",
      "accountNumber" -> "12345678",
      "sortCode" -> "123456",
      "rollNumber" -> "ROLL123"
    )

    def url(params: Map[String, String] = defaultParams): String = {
      val queryString: String = params match {
        case map if map.isEmpty => ""
        case nonEmptyMap => "?" + nonEmptyMap.toSeq.map((k, v) => s"$k=$v").mkString("&")
      }

      "/low-earners-pensions-payment/bank-account-details" + queryString
    }

    val requestJson: JsValue = Json.toJson(
      ValidatedBarsRequest(
        account = BarsAccount(
          sortCode = "123456",
          accountNumber = "12345678",
          rollNumber = Some("ROLL123")
        ),
        subject = BarsSubject(
          name = Some("aName")
        )
      )
    )

    "handle validation errors" when {
      "request body is missing mandatory fields" in {
        val application: Application = fakeApplication()
        runningRequest(application) {
          val fakeRequest = FakeRequest(GET, url(Map.empty))
          val result: Future[Result] = routeRequest(application, fakeRequest).getOrElse(
            Future.failed(new RuntimeException("TEST_ERROR"))
          )

          status(result) shouldBe Status.BAD_REQUEST
          contentAsString(result) should include("REQUEST_MISSING_MANDATORY_FIELD")
        }
      }

      "request body fails field validations" in {
        val application: Application = fakeApplication()
        runningRequest(application) {
          val queryParams: Map[String, String] = Map(
            "name" -> "!!!",
            "accountNumber" -> "1234",
            "sortCode" -> "text"
          )

          val fakeRequest = FakeRequest(GET, url(queryParams))
          val result: Future[Result] = routeRequest(application, fakeRequest).getOrElse(
            Future.failed(new RuntimeException("TEST_ERROR"))
          )

          status(result) shouldBe Status.BAD_REQUEST
          contentAsString(result) should include("REQUEST_FIELD_FORMAT_ERROR")
        }
      }
    }

    "handle connector errors" when {
      def handleBarsError(responseStatus: Int, resultStatus: Int, errorCode: String): Unit =
        s"BARS returns status - $responseStatus, should result in error with code - $errorCode" in {

          val application: Application = fakeApplication()

          runningRequest(application) {
            val fakeRequest = FakeRequest(GET, url())

            stubPost(
              url = "/verify/personal",
              requestBody = requestJson.toString,
              response = aResponse.withStatus(responseStatus)
            )

            val result: Future[Result] = routeRequest(application, fakeRequest).getOrElse(
              Future.failed(new RuntimeException("TEST_ERROR"))
            )

            status(result) shouldBe resultStatus
            contentAsString(result) should include(errorCode)
          }
        }

      Seq(
        /*
        TODO - These scenarios don't work as BARS redirects are followed by default        
        (MOVED_PERMANENTLY, INTERNAL_SERVER_ERROR, "BARS_RETURNED_REDIRECT"),
        (SEE_OTHER, INTERNAL_SERVER_ERROR, "BARS_RETURNED_REDIRECT"),
        (TEMPORARY_REDIRECT, INTERNAL_SERVER_ERROR, "BARS_RETURNED_REDIRECT"),
        */
        (BAD_REQUEST, INTERNAL_SERVER_ERROR, "ERROR_IN_BARS_REQUEST"),
        (NOT_FOUND, INTERNAL_SERVER_ERROR, "COULD_NOT_ACCESS_BARS_RESOURCE"),
        (FORBIDDEN, INTERNAL_SERVER_ERROR, "COULD_NOT_ACCESS_BARS_RESOURCE"),
        (INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR, "BARS_INTERNAL_SERVER_ERROR")
      ).foreach((responseStatus, resultStatus, errorCode) => handleBarsError(responseStatus, resultStatus, errorCode))

      "BARS returns invalid JSON" in {
        val application: Application = fakeApplication()

        runningRequest(application) {
          val fakeRequest = FakeRequest(GET, url())

          stubPost(
            url = "/verify/personal",
            requestBody = requestJson.toString,
            response = aResponse.withStatus(OK).withBody("not json")
          )

          val result: Future[Result] = routeRequest(application, fakeRequest).getOrElse(
            Future.failed(new RuntimeException("TEST_ERROR"))
          )

          status(result) shouldBe INTERNAL_SERVER_ERROR
          contentAsString(result) should include("FAILED_TO_PARSE_DOWNSTREAM_RESPONSE")
        }
      }

      "handle happy path" when {
        "BARS API returns a valid response" in {
          val application: Application = fakeApplication()

          runningRequest(application) {
            val fakeRequest = FakeRequest(GET, url())

            val responseBody: String =
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

            stubPost(
              url = "/verify/personal",
              requestBody = requestJson.toString,
              response = aResponse.withStatus(OK).withBody(responseBody)
            )

            val result: Future[Result] = routeRequest(application, fakeRequest).getOrElse(
              Future.failed(new RuntimeException("TEST_ERROR"))
            )

            status(result) shouldBe OK
            contentAsJson(result) shouldBe Json.parse(responseBody)
          }
        }
      }
    }
  }
}