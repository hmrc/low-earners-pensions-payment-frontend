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
import navigation.Navigator
import pages.{CheckYourAnswersPage, WhatAreYourBankDetailsPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{LeppSubmissionService, SessionCacheService}
import utils.CorrelationIdOptional
import viewmodels.NormalMode
import viewmodels.checkYourAnswers.CheckYourAnswersSummary.cyaSummaryList
import views.html.CheckYourAnswersView

import scala.concurrent.Future

@Singleton
class CheckYourAnswersController @Inject()(
                                            identify: IdentifierAction,
                                            getData: DataRetrievalAction,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView,
                                            claimSubmissionService: LeppSubmissionService,
                                            sessionService: SessionCacheService,
                                            correlationIdHandler: CorrelationIdOptional,
                                            navigator: Navigator
                                          ) extends LeppBaseController(identify, getData) with I18nSupport {
  def onPageLoad(): Action[AnyContent] = handle { implicit request =>
    request.userAnswers.get(WhatAreYourBankDetailsPage) match {
      case Some(accountDetails) =>
        Future.successful(Ok(view(cyaSummaryList(accountDetails), viewModel(NormalMode, CheckYourAnswersPage))))
      case None =>
        Future.successful(Redirect(routes.WhatAreYourBankDetailsController.onPageLoad(NormalMode)))
    }
  }
  
  def onSubmit(): Action[AnyContent] = handle { implicit request =>
    correlationIdHandler.handleCorrelationId(request){implicit id => 
      request.userAnswers.get(WhatAreYourBankDetailsPage) match {
        case Some(_) =>
          Future.successful(Redirect(navigator.nextPage(CheckYourAnswersPage, NormalMode)))
        case None =>
          Future.successful(Redirect(routes.WhatAreYourBankDetailsController.onPageLoad(NormalMode)))
      } 
    }
  }
}
