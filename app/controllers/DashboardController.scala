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
import models.userAnswers.LeppItemStatus.*
import models.userAnswers.{LeppItem, LeppSummary}
import navigation.Navigator
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SessionCacheService
import views.html.DashboardView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DashboardController @Inject()(identify: IdentifierAction,
                                    getData: DataRetrievalAction,
                                    val sessionService: SessionCacheService,
                                    val controllerComponents: MessagesControllerComponents,
                                    view: DashboardView,
                                    navigator: Navigator)
                                   (implicit val ec: ExecutionContext)
  extends LeppBaseController(identify, getData) with I18nSupport with SessionDataHandling {
  
  def onPageLoad(): Action[AnyContent] = handleWithSubmissionCheck { implicit request =>
    val tempData: LeppSummary = LeppSummary(
      currentLock = 67,
      Seq(
        LeppItem(
          taxYear = 2025,
          contributions = 1000,
          taxRate = 20,
          entitlement = 200,
          status = Available,
          claimDate = None
        ),
        LeppItem(
          taxYear = 2026,
          contributions = 750,
          taxRate = 20,
          entitlement = 150,
          status = Available,
          claimDate = None
        )
      )
    )
    
    Future.successful(Ok(view(tempData.items, None)))
  }

  def onSubmit(): Action[AnyContent] = handleWithSubmissionCheck { implicit request =>
    Future.successful(Ok(""))
  }
}

