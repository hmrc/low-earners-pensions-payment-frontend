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

import common.IntegrationSpecBase
import models.userAnswers.LeppItemStatus.Paid
import models.userAnswers.{LeppItem, LeppSummary}
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.SessionKeys
import views.html.DashboardView

import java.time.LocalDate
import scala.concurrent.Future

class DashboardControllerISpec extends ControllerIntegrationSpecBase {

  "GET /dashboard" when {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
      method = "GET",
      path = "/low-earners-pensions-payment/dashboard"
    ).withSession(SessionKeys.authToken -> "auth token")

    val ineligibleRoute: String = controllers.auth.routes.IneligibleController.onPageLoad().url

    def testForBackendResult(responseStatus: Int,
                             responseBody: JsValue,
                             resultStatus: Int,
                             resultBodyOpt: Option[String] = None,
                             redirectUrlOpt: Option[String] = None) = {
      lazy val application: Application = fakeApplication()

      when(
        method = GET,
        uri = "/low-earners-pensions-payment/get-payment-details"
      ).thenReturn(
        status = responseStatus,
        body = responseBody.toString
      )

      lazy val result: Future[Result] = route(application, request).getOrElse(
        Future.failed(new RuntimeException("TEST_ERROR"))
      )

      mockAuthSuccess()
      status(result) shouldBe resultStatus
      resultBodyOpt.foreach(body => contentAsString(result) shouldBe body)
      redirectLocation(result) shouldBe redirectUrlOpt
    }

    def testForBackendError(errorStatus: Int,
                            errorCode: String,
                            expectedRedirect: String): Unit =
      s"should return expected redirect for error status: $errorStatus, and error code: $errorCode" in {
        val json: JsValue = Json.parse(
          s"""
             |{
             | "code": "$errorCode"
             |}
          """.stripMargin
        )

        testForBackendResult(errorStatus, json, SEE_OTHER, redirectUrlOpt = Some(expectedRedirect))
      }

    "when call to LEPP backend endpoint returns an error response" should {
      val errorRoute: String = controllers.routes.SomethingWentWrongController.onPageLoad().url

      Seq(
        (BAD_REQUEST, "BAD_REQUEST", errorRoute),
        (NOT_FOUND, "NOT_FOUND", ineligibleRoute),
        (INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", errorRoute),
        (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", errorRoute),
      ).foreach(testForBackendError)
    }

    "when call to LEPP backend endpoint returns a success response" should {
      "redirect to ineligible page when lowEarnersDetailsList is empty" in {
        val json: JsValue = Json.parse(
          """
            |{
            | "currentLowEarnersOptimisticLock": 123,
            | "identifier": "NOT_USED",
            | "lowEarnersDetailsList": []
            |}
                """.stripMargin
        )

        testForBackendResult(
          responseStatus = OK,
          responseBody = json,
          resultStatus = SEE_OTHER,
          redirectUrlOpt = Some(ineligibleRoute)
        )
      }

      "redirect to ineligible page when lowEarnersDetailsList contains only unsupported items" in {
        val json: JsValue = Json.parse(
          """
            |{
            | "currentLowEarnersOptimisticLock": 123,
            | "identifier": "NOT_USED",
            | "lowEarnersDetailsList": [
            |   {
            |     "taxYear": 11,
            |     "lowEarnersCalculations": [
            |       {
            |         "lowEarnersClaimDetails": {
            |           "claimSequenceNumber": 123,
            |           "calculationDate": "2023-06-27",
            |           "claimDate": "2023-06-27",
            |           "claimStatus": "DECEASED - NO CAPACITOR",
            |           "entitlementAmount": 10.56,
            |           "inSelfAssessment": true,
            |           "originalAmount": 10.56,
            |           "reissueClaimOutput": true,
            |           "reminderOutputSent": true
            |         },
            |         "lowEarnersDataDetails": {
            |           "calculationSequenceNumber": 123,
            |           "basicRatePercentage": 10.56,
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
        )

        testForBackendResult(
          responseStatus = OK,
          responseBody = json,
          resultStatus = SEE_OTHER,
          redirectUrlOpt = Some(ineligibleRoute)
        )
      }

      "serve correct view for a valid response" in {
        val json: JsValue = Json.parse(
          """
            |{
            | "currentLowEarnersOptimisticLock": 123,
            | "identifier": "NOT_USED",
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
            |           "basicRatePercentage": 10.56,
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
        )

        lazy val application: Application = fakeApplication()

        when(
          method = GET,
          uri = "/low-earners-pensions-payment/get-payment-details"
        ).thenReturn(
          status = OK,
          body = json.toString
        )

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        val view: DashboardView = application.injector.instanceOf[DashboardView]
        implicit val messages: Messages = application.injector.instanceOf[MessagesApi].preferred(request)

        val backLink: String = routes.WhatYouWillNeedController.onPageLoad().url
        val continueUrl: String = routes.PaymentCalcBreakdownController.onPageLoad().url

        val leppSummaryModel: LeppSummary = LeppSummary(
          currentLock = 123,
          paidItems = Some(Seq(
            LeppItem(
              id = "P-11-1",
              taxYear = 11,
              contributions = 10.56,
              taxRate = 10.56,
              entitlement = 10.56,
              status = Paid,
              claimDate = Some(LocalDate.of(2023, 6, 27))
            )
          ))
        )

        mockAuthSuccess()
        status(result) shouldBe OK
        contentAsString(result) shouldEqual view(leppSummaryModel, Some(backLink), continueUrl).toString
      }
    }
  }

}