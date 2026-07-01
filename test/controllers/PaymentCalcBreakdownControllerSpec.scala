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

import base.SpecBase
import models.barsLockout.{BarsVerifyStatusResponse, NumberOfBarsVerifyAttempts}
import models.userAnswers.{LeppSummary, UserAnswers}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.NormalMode
import views.html.PaymentCalcBreakdownView

import scala.concurrent.Future

class PaymentCalcBreakdownControllerSpec extends SpecBase {
  
  "Payment calculation breakdown controller" - {
    when(mockBarsConnector.status()(
      ArgumentMatchers.any(),
      ArgumentMatchers.any(),
      ArgumentMatchers.any()
    )).thenReturn(Future.successful(BarsVerifyStatusResponse(NumberOfBarsVerifyAttempts(1), None)))

    val userAnswers = emptyUserAnswers.copy(
      data = Json.obj(
        "leppSummary" -> Json.toJson(summaryModel)
      ))
    
    "must return OK and the breakdown view of available items for a GET having data in the cache" in {
      val application = applicationBuilder(userAnswers).build()
      val view = application.injector.instanceOf[PaymentCalcBreakdownView]
      
      running(application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, controllers.routes.PaymentCalcBreakdownController.onPageLoad(None).url)
        
        val result = route(application, request).value
        contentAsString(result) mustEqual view(
          summaryModel,
          routes.WhatAreYourBankDetailsController.onPageLoad(NormalMode).url,
          Some(routes.DashboardController.onPageLoad().url)
        )(request, messages(application)).toString
        status(result) mustEqual OK
      }
    }
    
    "must return OK and the breakdown view of history item for a GET having data in the cache" in {

      val application = applicationBuilder(userAnswers).build()
      val view = application.injector.instanceOf[PaymentCalcBreakdownView]
      running(application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, controllers.routes.PaymentCalcBreakdownController.onPageLoad(Some("P-25-1")).url)

        val result = route(application, request).value
        contentAsString(result) mustEqual view(
          summaryModel,
          routes.WhatAreYourBankDetailsController.onPageLoad(NormalMode).url,
          Some(routes.DashboardController.onPageLoad().url),
          false,
          Some("P-25-1")
        )(request, messages(application)).toString
        status(result) mustEqual OK
      }
    }

    "must redirect to dashboard page when no summary data in the cache" in {
      val application = applicationBuilder().build()
      when(mockBarsConnector.status()(
        ArgumentMatchers.any(),
        ArgumentMatchers.any(),
        ArgumentMatchers.any()
      )).thenReturn(Future.successful(BarsVerifyStatusResponse(NumberOfBarsVerifyAttempts(1), None)))
      
      running(application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, controllers.routes.PaymentCalcBreakdownController.onPageLoad(None).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.DashboardController.onPageLoad().url)
      }
    }
  }
}
