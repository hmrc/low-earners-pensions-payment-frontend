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
import controllers.common.LeppBaseController
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SessionCacheService‘

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ClearCacheController @Inject()(override val messagesApi: MessagesApi,
                                     val controllerComponents: MessagesControllerComponents,
                                     identify: IdentifierAction,
                                     getData: DataRetrievalAction,
                                     sessionCacheService: SessionCacheService)
                                    (using ExecutionContext) extends LeppBaseController(identify, getData) {

  def onPageLoad(): Action[AnyContent] = handle { implicit request =>
    sessionCacheService
      .clear(request.userAnswers)
      .map { _ =>
        Redirect(routes.DashboardController.onPageLoad().url)
      }
  }

  def defaultError(): Action[AnyContent] = handle { implicit request =>
    sessionCacheService
      .clear(request.userAnswers)
      .map { _ =>
        Redirect(routes.SomethingWentWrongController.onPageLoad())
      }
  }
}
