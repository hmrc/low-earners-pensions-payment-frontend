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

import com.google.inject.{Inject, Singleton}
import controllers.actions.{BarsLockoutAction, DataRetrievalAction, IdentifierAction}
import forms.WhatAreYourBankDetailsFormProvider
import models.userAnswers.BankAccountDetails
import navigation.Navigator
import pages.WhatAreYourBankDetailsPage
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.*
import utils.Logging
import viewmodels.Mode
import views.html.WhatAreYourBankDetailsView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatAreYourBankDetailsController @Inject()(identify: IdentifierAction,
                                                 barsLockout: BarsLockoutAction,
                                                 getData: DataRetrievalAction,
                                                 val sessionService: SessionCacheService,
                                                 formProvider: WhatAreYourBankDetailsFormProvider,
                                                 view: WhatAreYourBankDetailsView,
                                                 navigator: Navigator,
                                                 val controllerComponents: MessagesControllerComponents)
                                                (implicit val ec: ExecutionContext)
  extends BarsLeppBaseController(identify, getData, barsLockout) with I18nSupport with SessionDataHandling with Logging {

  private val form: Form[BankAccountDetails] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = handleWithLeppData { implicit req =>
    _ =>
      req.userAnswers.get(WhatAreYourBankDetailsPage) match {
        case Some(value) => Future.successful(Ok(view(form.fill(value), viewModel(mode, WhatAreYourBankDetailsPage))))
        case None => Future.successful(Ok(view(form, viewModel(mode, WhatAreYourBankDetailsPage))))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = handle { implicit req =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(
          view(formWithErrors, viewModel(mode, WhatAreYourBankDetailsPage))
        )),
        answer => {
          for {
            updatedAnswers <- Future.fromTry(req.userAnswers.set(WhatAreYourBankDetailsPage, answer))
            _ <- sessionService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage(WhatAreYourBankDetailsPage, mode))
        }
      )
  }
}
