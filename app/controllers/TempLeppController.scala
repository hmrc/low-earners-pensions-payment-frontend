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
import models.userAnswers.LeppItemStatus.Available
import models.userAnswers.{LeppItem, LeppSummary}
import navigation.Navigator
import pages.TempPage.*
import pages.{BreakdownPage, ConfirmationPage, DashboardPage, Page, TempPage, WhatAreYourBankDetailsPage}
import play.api.i18n.I18nSupport
import play.api.libs.json.JsObject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SessionCacheService
import viewmodels.NormalMode
import views.html.TempLeppView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class TempLeppController @Inject()(identify: IdentifierAction,
                                   getData: DataRetrievalAction,
                                   val controllerComponents: MessagesControllerComponents,
                                   tempView: TempLeppView,
                                   sessionService: SessionCacheService,
                                   navigator: Navigator)
                                  (implicit ec: ExecutionContext)
  extends LeppBaseController(identify, getData) with I18nSupport {

  private def toPageModel(tempPage: TempPage): Page = tempPage match {
    case Dashboard => DashboardPage
    case Breakdown => BreakdownPage
    case Confirmation => ConfirmationPage
  }

  def onPageLoad(tempPage: TempPage): Action[AnyContent] = handle { implicit request =>
    val result: Result = Ok(tempView(tempPage, viewModel(NormalMode, toPageModel(tempPage))))

    tempPage match {
      case TempPage.Dashboard =>
        for {
          _ <- sessionService.save(request.userAnswers.copy(data = JsObject.empty))
        } yield result
      case _ => Future.successful(result)
    }
  }

  def onSubmit(tempPage: TempPage): Action[AnyContent] = handle { implicit request =>
    val result: Result = Redirect(navigator.nextPage(toPageModel(tempPage), NormalMode))

    tempPage match {
      case TempPage.Dashboard =>
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
      case _ => Future.successful(result)
    }
  }
}
  