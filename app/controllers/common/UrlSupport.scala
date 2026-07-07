package controllers.common

import controllers.routes
import pages.{BarsRequestErrorsPage, CheckYourAnswersPage, DashboardPage, Page, PaymentCalcBreakdownPage, SubmissionPage, WhatAreYourBankDetailsPage, WhatYouWillNeedPage}
import play.api.mvc.Call
import viewmodels.{CheckMode, Mode}
import viewmodels.formPages.FormPageViewModel

trait UrlSupport {
  protected def viewModel(mode: Mode, page: Page): FormPageViewModel =
    FormPageViewModel(
      onSubmit = submitUrl(mode, page),
      backLinkUrl = Some(backLinkUrl(mode, page).url)
    )

  protected[controllers] def submitUrl(mode: Mode, page: Page): Call = page match {
    case WhatAreYourBankDetailsPage => routes.WhatAreYourBankDetailsController.onSubmit(mode)
    case BarsRequestErrorsPage => routes.WhatAreYourBankDetailsController.onSubmit(mode)
    case CheckYourAnswersPage => routes.CheckYourAnswersController.onSubmit()
    case _ => routes.DashboardController.onPageLoad()
  }

  protected[controllers] def backLinkUrl(mode: Mode, page: Page): Call = {
    val backPage: Page = (mode, page) match {
      case (CheckMode, _) => CheckYourAnswersPage
      case (_, DashboardPage) => WhatYouWillNeedPage
      case (_, PaymentCalcBreakdownPage) => DashboardPage
      case (_, WhatAreYourBankDetailsPage) => PaymentCalcBreakdownPage
      case (_, CheckYourAnswersPage) => WhatAreYourBankDetailsPage
      case (_, SubmissionPage) => DashboardPage
      case _ => WhatYouWillNeedPage
    }
    backPage.route(mode)
  }
}
