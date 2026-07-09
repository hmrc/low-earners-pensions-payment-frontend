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

package controllers.common

import base.SpecBase
import pages.*
import viewmodels.{CheckMode, Mode, NormalMode}

class UrlSupportSpec extends SpecBase {
  
  private object UrlSupportTest extends UrlSupport

  "submitUrl" - {
    def testSubmitUrl(mode: Mode, page: Page, link: String): Unit =
      s"should return correct submit link for page: $page, and mode: $mode" in {
        UrlSupportTest.submitUrl(mode, page).url mustBe link
      }

    Seq(
      (NormalMode, WhatAreYourBankDetailsPage, "/accept-your-low-earners-pension-payment/bank-details"),
      (CheckMode, WhatAreYourBankDetailsPage, "/accept-your-low-earners-pension-payment/change-bank-details"),
      (NormalMode, CheckYourAnswersPage, "/accept-your-low-earners-pension-payment/check-your-answers")
    ).foreach(testSubmitUrl)
  }

  "backLinkUrl" - {
    def testBackLink(mode: Mode, page: Page, link: String): Unit =
      s"should return correct back link for page: ${page.toString}, and mode: $mode" in {
        UrlSupportTest.backLinkUrl(mode, page).url mustBe link
      }

    Seq(
      (NormalMode, DashboardPage, "/accept-your-low-earners-pension-payment/start"),
      (NormalMode, PaymentCalcBreakdownPage, "/accept-your-low-earners-pension-payment/payments"),
      (NormalMode, WhatAreYourBankDetailsPage, "/accept-your-low-earners-pension-payment/payment-breakdown"),
      (CheckMode, WhatAreYourBankDetailsPage, "/accept-your-low-earners-pension-payment/check-your-answers"),
      (NormalMode, CheckYourAnswersPage, "/accept-your-low-earners-pension-payment/bank-details")
    ).foreach(testBackLink)
  }
}
