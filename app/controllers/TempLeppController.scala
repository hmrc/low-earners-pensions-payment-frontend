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

import controllers.actions.{BarsLockoutAction, DataRetrievalAction, IdentifierAction}
import models.userAnswers.LeppItemStatus.Available
import models.userAnswers.{LeppItem, LeppSummary}
import navigation.Navigator
import pages.*
import play.api.i18n.I18nSupport
import play.api.libs.json.JsObject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SessionCacheService
import viewmodels.NormalMode
import views.html.TempLeppView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TempLeppController @Inject()(identify: IdentifierAction,
                                   getData: DataRetrievalAction,
                                   val controllerComponents: MessagesControllerComponents,
                                   tempView: TempLeppView,
                                   sessionService: SessionCacheService,
                                   navigator: Navigator)
                                  (implicit ec: ExecutionContext)
  extends LeppBaseController(identify, getData) with I18nSupport {
  
  def onPageLoad(tempPage: TempPage): Action[AnyContent] = handle { implicit request =>
    val result: Result = Ok(tempView(tempPage, viewModel(NormalMode, DashboardPage)))

    for {
      _ <- sessionService.save(request.userAnswers.copy(data = JsObject.empty))
    } yield result
    
    Future.successful(result)
  }

  def onSubmit(tempPage: TempPage): Action[AnyContent] = handle { implicit request =>
    val result: Result = Redirect(navigator.nextPage(DashboardPage, NormalMode))
    
         // placeholder for NPS integration
        val tempData: LeppSummary = LeppSummary(
          currentLock = 67,
          Seq(
            LeppItem(
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Available
            ),
            LeppItem(
              taxYear = 2026,
              contributions = 750,
              taxRate = 20,
              entitlement = 150,
              status = Available
            )
          )
        )
    
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(DashboardPage, tempData))
          _ <- sessionService.save(updatedAnswers)
        } yield result
      Future.successful(result)
  }
}
  