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
import models.userAnswers.{LeppItem, LeppSummary, UserAnswers}
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class PaymentCalcBreakdownControllerSpec extends SpecBase {

  val summaryModel: LeppSummary = LeppSummary(
    currentLock = 67,
    Seq(
      LeppItem(
        taxYear = 2025,
        contributions = 1000,
        taxRate = 20,
        entitlement = 200,
        status = Available
      ),
      LeppItem(
        taxYear = 2026,
        contributions = 750,
        taxRate = 20,
        entitlement = 150,
        status = Available
      )
    )
  )
  
  "Payment calculation breakdown controller" - {
    "must return OK and the breakdown view for a GET having data in the cache" in {
      val userAnswers = emptyUserAnswers.copy(
        data = Json.obj(
          "leppSummary" -> Json.toJson(summaryModel)
        ))
      val application = applicationBuilder(userAnswers).build()

      running(application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, controllers.routes.PaymentCalcBreakdownController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to dashboard page when no summary data in the cache" in {
      val application = applicationBuilder().build()

      running(application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] =
          FakeRequest(GET, controllers.routes.PaymentCalcBreakdownController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.TempLeppController.onPageLoad().url)
      }
    }
  }
}
