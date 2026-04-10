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
import pages.{Page, WhatAreYourBankDetailsPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.Mode
import viewmodels.formPages.FormPageViewModel

import javax.inject.Inject
import scala.concurrent.Future

abstract class LeppBaseController  @Inject()(identify: IdentifierAction,
                                             getData: DataRetrievalAction) extends FrontendBaseController with I18nSupport:

  protected def viewModel(mode: Mode, page: Page): FormPageViewModel =
    FormPageViewModel(
      onSubmit = submitUrl(mode, page),
      backLinkUrl = Some(backLinkUrl(mode, page))
    )

  protected def submitUrl(mode: Mode, page: Page): Call = page match {
    case WhatAreYourBankDetailsPage => routes.WhatAreYourBankDetailsController.onSubmit()
    case _ => routes.WhatYouWillNeedController.onPageLoad() //Placeholder to avoid warnings
  }

  private def backLinkUrl(mode: Mode, page: Page): String = page match {
    case WhatAreYourBankDetailsPage => routes.WhatYouWillNeedController.onPageLoad().url
    case _ => routes.WhatYouWillNeedController.onPageLoad().url //Placeholder to avoid warnings
  }

  def handle(f: DataRequest[AnyContent] => Future[Result]): Action[AnyContent] = (identify andThen getData).async :
    implicit request => f(request)
