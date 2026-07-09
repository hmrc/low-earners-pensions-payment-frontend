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
import controllers.actions.fakes.FakeRedirectBarsLockoutAction
import controllers.actions.{BarsLockoutAction, RedirectBarsLockoutAction}
import models.userAnswers.{BankAccountDetails, LeppSummary, SubmissionSummary, UserAnswers}
import pages.{DashboardPage, WhatAreYourBankDetailsPage}
import play.api.Application
import play.api.libs.json.{JsBoolean, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.NormalMode

class SubmitConfirmationControllerSpec extends SpecBase {

  "Submit confirmation controller" - {
    val bankAccountDetails: BankAccountDetails = BankAccountDetails(
      accountName = "name",
      accountNumber = "number",
      sortCode = "sortcode",
      rollNumber = Some("rollNumber")
    )

    val userAnswers: UserAnswers = UserAnswers(
      id = "1",
      data = Json.obj(
        "leppSummary" -> Json.toJson(summaryModel),
        "bankDetails" -> Json.toJson(bankAccountDetails),
        "isSubmitted" -> JsBoolean(true)
      )
    )
    
    "must redirect to ClearCacheController" in {
      val application = applicationBuilder(userAnswers = userAnswers).build()

      running(application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, controllers.routes.SubmitConfirmationController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.ClearCacheController.onPageLoad().url)
      }
    }

    "must return OK and the correct view for a GET" in {
      val userAnswers: UserAnswers = UserAnswers(
        id = "1",
        data = Json.obj(
          "leppSummary" -> Json.toJson(summaryModel),
          "bankDetails" -> Json.toJson(bankAccountDetails),
          "leppSubmissionSummary" -> Json.toJson(SubmissionSummary(Seq("A-25-1")))
        )
      )
      
      val application = applicationBuilder(userAnswers = userAnswers).build()

      running(application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
          GET,
          controllers.routes.SubmitConfirmationController.onPageLoad().url
        )

        val result = route(application, request).value
        status(result) mustEqual OK
      }
    }

    "must redirect to checkYourAnswers controller if the submission is not yet done" in {

      val nonSubmissionUserAnswers: UserAnswers =
        emptyUserAnswers.set(page = DashboardPage, value = summaryModel).success.value
          .set(page = WhatAreYourBankDetailsPage, value = bankAccountDetails).success.value
          
      val application = applicationBuilder(userAnswers = nonSubmissionUserAnswers).build()

      running(application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, controllers.routes.SubmitConfirmationController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.CheckYourAnswersController.onPageLoad().url)
      }
    }

    "must redirect to BARS lockout controller when bars check limit exceeds" in {
      val mockRedirectBarsLockoutAction: RedirectBarsLockoutAction = FakeRedirectBarsLockoutAction(3)
      
      val application: Application = applicationBuilder(
        userAnswers = userAnswers,
        redirectBarsLockoutAction = mockRedirectBarsLockoutAction
      ).build()

      running(application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, controllers.routes.SubmitConfirmationController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(bars.routes.BarsLockoutController.onPageLoad().url)
      }
    }

    "must redirect to bank details controller when banks details are empty" in {
      val noBankDetailsUserAnswers: UserAnswers =
        emptyUserAnswers.set(page = DashboardPage, value = summaryModel).success.value
        
      val application = applicationBuilder(userAnswers = noBankDetailsUserAnswers).build()

      running(application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, controllers.routes.SubmitConfirmationController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.WhatAreYourBankDetailsController.onPageLoad(NormalMode).url)
      }
    }
  }
}
