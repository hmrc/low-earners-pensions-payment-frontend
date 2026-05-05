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

package controllers.bars

import com.google.inject.{Inject, Singleton}
import controllers.LeppBaseController
import controllers.actions.{Actions, DataRetrievalAction}
import pages.BarsRequestErrorsPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import viewmodels.NormalMode
import views.html.bars.BarsRequestErrorsView

import scala.concurrent.Future

@Singleton
class BarsRequestErrorsController @Inject()(actions: Actions,
                                            getData: DataRetrievalAction,
                                            view: BarsRequestErrorsView,
                                            val controllerComponents: MessagesControllerComponents)
  extends LeppBaseController(actions, getData) with I18nSupport {
  def onPageLoad(): Action[AnyContent] = handle { implicit request =>
    Future.successful(BadRequest(view(viewModel(NormalMode, BarsRequestErrorsPage))))
  }
}
