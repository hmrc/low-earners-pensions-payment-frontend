package controllers.common

import base.SpecBase
import viewmodels.Mode
import viewmodels.{NormalMode, CheckMode}
import pages.*

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
