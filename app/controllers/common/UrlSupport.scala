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

import controllers.routes
import pages.*
import play.api.mvc.Call
import viewmodels.formPages.FormPageViewModel
import viewmodels.{CheckMode, Mode}

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
