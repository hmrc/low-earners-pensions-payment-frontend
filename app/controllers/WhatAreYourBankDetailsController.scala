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
import controllers.actions.{DataRetrievalAction, IdentifierAction}
import forms.WhatAreYourBankDetailsFormProvider
import models.userAnswers.BankAccountDetails
import navigation.Navigator
import pages.WhatAreYourBankDetailsPage
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.*
import utils.CorrelationIdOptional
import viewmodels.Mode
import views.html.WhatAreYourBankDetailsView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatAreYourBankDetailsController @Inject()(identify: IdentifierAction,
                                                 getData: DataRetrievalAction,
                                                 correlationIdHandler: CorrelationIdOptional,
                                                 formProvider: WhatAreYourBankDetailsFormProvider,
                                                 view: WhatAreYourBankDetailsView,
                                                 barsService: BarsService,
                                                 sessionService: SessionCacheService,
                                                 navigator: Navigator,
                                                 val controllerComponents: MessagesControllerComponents)
                                                (implicit ec: ExecutionContext)
  extends LeppBaseController(identify, getData) with I18nSupport {

  private val form: Form[BankAccountDetails] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = handle { implicit request =>
    request.userAnswers.get(WhatAreYourBankDetailsPage) match {
      case Some(value) => Future.successful(Ok(view(form.fill(value), viewModel(mode, WhatAreYourBankDetailsPage))))
      case None => Future.successful(Ok(view(form, viewModel(mode, WhatAreYourBankDetailsPage))))
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = handle { implicit request =>
    correlationIdHandler.handleCorrelationId(request)(correlationId =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(
            view(formWithErrors, viewModel(mode, WhatAreYourBankDetailsPage))
          )),
          answer => {
            barsService.checkBankAccountDetails(
              barsRequest = answer.toBarsRequest,
              correlationId = correlationId
            ).biSemiflatMap(
              err => if(err.value.status == BAD_REQUEST) {
                Future.successful(Redirect(controllers.bars.routes.BarsRequestErrorsController.onPageLoad()))
              } else {
                Future.successful(Redirect(controllers.bars.routes.BarsCheckFailedController.onPageLoad()))
              },
              _ => for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(WhatAreYourBankDetailsPage, answer))
                _ <- sessionService.save(updatedAnswers)
              } yield Redirect(navigator.nextPage(WhatAreYourBankDetailsPage, mode))
            ).merge
          }
        )
    )
  }
}
