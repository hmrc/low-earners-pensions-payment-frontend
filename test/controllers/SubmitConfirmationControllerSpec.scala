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
import models.userAnswers.LeppItemStatus.Available
import models.userAnswers.{BankAccountDetails, LeppItem, LeppSummary, UserAnswers}
import play.api.libs.json.{JsBoolean, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class SubmitConfirmationControllerSpec extends SpecBase {

  "Submit confirmation controller" - {
    val summaryModel: LeppSummary = LeppSummary(
      currentLock = 67,
      availableItems = Seq(
        LeppItem(
          id = "A-25-1",
          taxYear = 2025,
          contributions = 1000,
          taxRate = 20,
          entitlement = 200,
          status = Available,
          claimDate = None
        )
      )
    )

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
    
    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = userAnswers).build()

      running(application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, controllers.routes.SubmitConfirmationController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }
  }
}
