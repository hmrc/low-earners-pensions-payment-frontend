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
import models.userAnswers.{BankAccountDetails, LeppSummary}
import pages.TempPage.{Breakdown, CheckYourAnswers}
import pages.*
import models.userAnswers.{BankAccountDetails, ClaimsSummary}
import pages.*
import play.api.libs.json.Reads
import services.SessionCacheService
import pages.TempPage.{Breakdown, Dashboard}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, Result}
import queries.Gettable
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.formPages.FormPageViewModel
import viewmodels.{CheckMode, Mode}
import viewmodels.{CheckMode, Mode, NormalMode}

import javax.inject.Inject
import scala.concurrent.Future

abstract class LeppBaseController @Inject()(identify: IdentifierAction,
                                            getData: DataRetrievalAction) extends FrontendBaseController with I18nSupport:

  protected def viewModel(mode: Mode, page: Page): FormPageViewModel =
    FormPageViewModel(
      onSubmit = submitUrl(mode, page),
      backLinkUrl = Some(backLinkUrl(mode, page).url)
    )

  protected[controllers] def submitUrl(mode: Mode, page: Page): Call = page match {
    case WhatYouWillNeedPage => routes.WhatYouWillNeedController.onPageLoad()
    case DashboardPage => routes.TempLeppController.onSubmit(Dashboard)
    case BreakdownPage => routes.TempLeppController.onSubmit(Breakdown)
    case WhatAreYourBankDetailsPage => routes.WhatAreYourBankDetailsController.onSubmit(mode)
    case BarsRequestErrorsPage => routes.WhatAreYourBankDetailsController.onSubmit(mode)
    case CheckYourAnswersPage => routes.CheckYourAnswersController.onSubmit()
    case _ => routes.TempLeppController.onPageLoad(Dashboard)
  }

  protected[controllers] def backLinkUrl(mode: Mode, page: Page): Call = {
    val backPage: Page = (mode, page) match {
      case (CheckMode, _) => CheckYourAnswersPage
      case (_, DashboardPage) => WhatYouWillNeedPage
      case (_, BreakdownPage) => DashboardPage
      case (_, WhatAreYourBankDetailsPage) => BreakdownPage
      case (_, CheckYourAnswersPage) => WhatAreYourBankDetailsPage
      case (_, ConfirmationPage) => DashboardPage
      case _ => WhatYouWillNeedPage
    }
    backPage.route(mode)
  }

  def handle(f: DataRequest[AnyContent] => Future[Result]): Action[AnyContent] = (identify andThen getData).async:
    implicit request => f(request)
    
  private type BlockFor[A] = DataRequest[AnyContent] => A => Future[Result]
  
  def handleWithData(f: BlockFor[LeppSummary]): Action[AnyContent] = handle { implicit req =>
    req.userAnswers.get(DashboardPage) match {
      case Some(claims) => f(req)(claims)
      case None => DashboardPage.asRedirect
    }
  }

  def handleWithDataAndDetails(f: BlockFor[BankAccountDetails]): Action[AnyContent] = handle { implicit req =>
    import req.userAnswers

    (userAnswers.get(DashboardPage), userAnswers.get(WhatAreYourBankDetailsPage)) match {
      case (Some(_), Some(details)) => f(req)(details)
      case (None, _) => DashboardPage.asRedirect
      case (_, None) => WhatAreYourBankDetailsPage.asRedirect
    }
  }
