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

import controllers.actions.{DataRetrievalAction, IdentifierAction}
import models.requests.DataRequest
import pages.TempPage.{Breakdown, CheckYourAnswers}
import pages.*
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.formPages.FormPageViewModel
import viewmodels.{CheckMode, Mode}

import javax.inject.Inject
import scala.concurrent.Future

abstract class LeppBaseController  @Inject()(identify: IdentifierAction,
                                             getData: DataRetrievalAction) extends FrontendBaseController with I18nSupport:

  protected def viewModel(mode: Mode, page: Page): FormPageViewModel =
    FormPageViewModel(
      onSubmit = submitUrl(mode, page),
      backLinkUrl = Some(backLinkUrl(mode, page).url)
    )

  protected def submitUrl(mode: Mode, page: Page): Call = page match {
    case BreakdownPage => routes.TempLeppController.onSubmit(Breakdown)
    case WhatAreYourBankDetailsPage => routes.WhatAreYourBankDetailsController.onSubmit(mode)
    case BarsRequestErrorsPage => routes.WhatAreYourBankDetailsController.onSubmit(mode)
    case CheckYourAnswersPage => routes.TempLeppController.onSubmit(CheckYourAnswers)
    case _ => routes.WhatYouWillNeedController.onPageLoad() //Placeholder to avoid warnings
  }

  private def backLinkUrl(mode: Mode, page: Page): Call = {
    val backPage: Page = (mode, page) match {
      case (CheckMode, _ ) => CheckYourAnswersPage
      case (_, WhatAreYourBankDetailsPage) => BreakdownPage
      case (_, BreakdownPage) => WhatYouWillNeedPage
      case _ => WhatYouWillNeedPage
    }
    backPage.route(mode)
  }

  def handle(f: DataRequest[AnyContent] => Future[Result]): Action[AnyContent] = (identify andThen getData).async :
    implicit request => f(request)
