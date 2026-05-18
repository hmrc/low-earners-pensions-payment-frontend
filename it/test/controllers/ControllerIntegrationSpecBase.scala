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

import common.{AuthSupport, IntegrationSpecBase}
import models.userAnswers.LeppItemStatus.Available
import models.userAnswers.{BankAccountDetails, LeppItem, LeppSummary, UserAnswers}
import play.api.Application
import play.api.http.Status.SEE_OTHER
import play.api.http.Writeable
import play.api.libs.json.*
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.NormalMode

import java.time.Instant
import scala.concurrent.Future

trait ControllerIntegrationSpecBase extends IntegrationSpecBase with AuthSupport {
  val summaryModel: LeppSummary = LeppSummary(
    currentLock = 67,
    availableItems = Some(Seq(
      LeppItem(
        id = "A-25-1",
        taxYear = 2025,
        contributions = 1000,
        taxRate = 20,
        entitlement = 200,
        status = Available,
        claimDate = None
      )
    )),
    paidItems = Some(Seq(
      LeppItem(
        id = "P-25-1",
        taxYear = 2025,
        contributions = 1000,
        taxRate = 20,
        entitlement = 200,
        status = Paid,
        claimDate = None
      )
    )),
    suspendedItems = Some(Seq(
      LeppItem(
        id = "S-25-1",
        taxYear = 2025,
        contributions = 1000,
        taxRate = 20,
        entitlement = 200,
        status = Suspended,
        claimDate = None
      )
    )),
    cancelledItems = Some(Seq(
      LeppItem(
        id = "C-25-1",
        taxYear = 2025,
        contributions = 1000,
        taxRate = 20,
        entitlement = 200,
        status = Cancelled,
        claimDate = None
      )
    )
    ))

  val bankAccountDetails: BankAccountDetails = BankAccountDetails(
    accountName = "Taxwell Payer",
    accountNumber = "12345678",
    sortCode = "112233",
    rollNumber = Some("1234678")
  )

  val emptyUserAnswers = UserAnswers(
    id = "some-id",
    data = JsObject.empty,
    lastUpdated = Instant.MIN
  )

  val userAnswersWithLeppSummary: UserAnswers = emptyUserAnswers.copy(
    data = Json.obj(
      "leppSummary" -> Json.toJson(summaryModel)
    )
  )

  val userAnswersWithBankDetails: UserAnswers = emptyUserAnswers.copy(
    data = Json.obj(
      "leppSummary" -> Json.toJson(summaryModel),
      "bankDetails" -> Json.toJson(bankAccountDetails)
    )
  )

  val userAnswersWithExistingSubmission: UserAnswers = UserAnswers(
    id = "1",
    data = Json.obj(
      "leppSummary" -> Json.toJson(summaryModel),
      "bankDetails" -> Json.toJson(bankAccountDetails),
      "isSubmitted" -> JsBoolean(true)
    )
  )


  private def dataTest[A: Writeable](scenario: String,
                                     expectedResult: String,
                                     request: FakeRequest[A],
                                     leppSummaryOpt: Option[LeppSummary] = Some(summaryModel),
                                     bankDetailsOpt: Option[BankAccountDetails] = Some(bankAccountDetails),
                                     isSubmittedOpt: Option[Boolean] = None,
                                     expectedRedirect: String): Unit =
    s"$scenario" must {
      s"$expectedResult" in {
        lazy val leppSummaryJson: JsObject = leppSummaryOpt.fold(JsObject.empty)(
          leppSummary => Json.obj("leppSummary" -> Json.toJson(leppSummary))
        )

        lazy val bankDetailsJson: JsObject = bankDetailsOpt.fold(JsObject.empty)(
          bankDetails => Json.obj("bankDetails" -> Json.toJson(bankDetails))
        )

        lazy val isSubmittedJson: JsObject = isSubmittedOpt.fold(JsObject.empty)(
          isSubmitted => Json.obj("isSubmitted" -> JsBoolean(isSubmitted))
        )

        lazy val userAnswers = UserAnswers(
          id = "some-id",
          data = leppSummaryJson ++ bankDetailsJson ++ isSubmittedJson,
          lastUpdated = Instant.MIN
        )

        lazy val application: Application = applicationWithUserAnswers(userAnswers)

        lazy val result: Future[Result] = route(application, request).getOrElse(
          Future.failed(new RuntimeException("TEST_ERROR"))
        )

        mockAuthSuccess()
        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(expectedRedirect)
      }
    }

  def testUserAnswersHandling[A: Writeable](request: FakeRequest[A],
                                            withLeppDataHandlingTest: Boolean = true,
                                            withBankDetailsHandlingTest: Boolean = false,
                                            submissionShouldExist: Boolean = false): Unit = {
    def existingSubmissionHandlingTest(): Unit = {
      if (submissionShouldExist) {
        dataTest(
          scenario = "an existing submission is required and missing from user answers",
          expectedResult = "redirect to CheckYourAnswers page",
          request = request,
          isSubmittedOpt = None,
          expectedRedirect = routes.CheckYourAnswersController.onPageLoad().url
        )
      } else {
        dataTest(
          scenario = "an existing submission has already been made",
          expectedResult = "wipe user answers and redirect to Dashboard page",
          request = request,
          isSubmittedOpt = Some(true),
          expectedRedirect = routes.TempLeppController.onPageLoad().url
        )
      }
    }

    "checking user answers" when {
      existingSubmissionHandlingTest()
      if (withLeppDataHandlingTest) {
        dataTest(
          request = request,
          scenario = "LeppSummary is required and is missing from user answers",
          expectedResult = "redirect to Dashboard page",
          leppSummaryOpt = None,
          expectedRedirect = routes.TempLeppController.onPageLoad().url
        )
      }
      if (withBankDetailsHandlingTest) {
        dataTest(
          request = request,
          scenario = "BankDetails are required and are missing from user answers",
          expectedResult = "redirect to BankDetails page",
          bankDetailsOpt = None,
          expectedRedirect = routes.WhatAreYourBankDetailsController.onPageLoad(NormalMode).url
        )
      }
    }
  }
}
