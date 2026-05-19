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
import controllers.actions.{BarsLockoutAction, DataRetrievalAction, IdentifierAction, Actions}
import navigation.Navigator
import pages.CheckYourAnswersPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{LeppSubmissionService, SessionCacheService}
import utils.CorrelationIdOptional
import viewmodels.NormalMode
import viewmodels.checkYourAnswers.CheckYourAnswersSummary.cyaSummaryList
import views.html.{CheckYourAnswersView, ErrorTemplate}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckYourAnswersController @Inject()(identify: IdentifierAction,
                                           barsLockout: BarsLockoutAction,
                                           getData: DataRetrievalAction,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: CheckYourAnswersView,
                                           leppSubmissionService: LeppSubmissionService,
                                           val sessionService: SessionCacheService,
                                           correlationIdHandler: CorrelationIdOptional,
                                           navigator: Navigator,
                                           errorView: ErrorTemplate)
                                          (implicit val ec: ExecutionContext)
  extends BarsLeppBaseController(identify, getData, barsLockout) with I18nSupport with SessionDataHandling {

  def onPageLoad(): Action[AnyContent] = handleForCyaPage { implicit req =>
    (_, bankDetails) =>
      Future.successful(Ok(
        view(
          summaryList = cyaSummaryList(bankDetails),
          viewModel = viewModel(NormalMode, CheckYourAnswersPage)
        )
      ))
  }

  def onSubmit(): Action[AnyContent] = handleForCyaPage { implicit req =>
    (leppData, bankDetails) =>
      correlationIdHandler.handleCorrelationId(req) { implicit cid =>
        leppSubmissionService.submitMultiple(leppData, bankDetails).biSemiflatMap(
          err =>
            for {
              _ <- sessionService.clear(req.userAnswers)
            } yield InternalServerError(errorView("title", "heading", "message")), 
          //TODO - Need to write content for this page
          //TODO - should probably implement ClearCacheController like in MPE
          _ =>
            for {
              updatedAnswers <- Future.fromTry(req.userAnswers.set(CheckYourAnswersPage, true))
              _ <- sessionService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(CheckYourAnswersPage, NormalMode))
        ).merge
      }
  }
}
