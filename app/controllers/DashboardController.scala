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

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import connectors.BarsVerifyStatusConnector
import controllers.actions.{DataRetrievalAction, IdentifierAction, NoRedirectBarsLockoutAction, StartPageCheckEligibilityAction, StartPageCheckEligibilityActionBuilder}
import controllers.common.{BarsLeppBaseController, EligibleLeppBaseController, LeppBaseController}
import models.ResponseWrapper.ErrorWrapper
import models.errors.ErrorResult.notEligibleError
import models.userAnswers.LeppSummary
import models.{CorrelationId, ResponseWrapper}
import navigation.Navigator
import pages.DashboardPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{LeppRetrievalService, SessionCacheService}
import uk.gov.hmrc.play.language.LanguageUtils
import utils.CorrelationIdHandler
import viewmodels.NormalMode
import views.html.DashboardView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DashboardController @Inject()(identify: IdentifierAction,
                                    barsLockout: NoRedirectBarsLockoutAction,
                                    getData: DataRetrievalAction,
                                    checkEligibility: StartPageCheckEligibilityActionBuilder,
                                    val sessionService: SessionCacheService,
                                    val controllerComponents: MessagesControllerComponents,
                                    view: DashboardView,
                                    navigator: Navigator)
                                   (implicit val ec: ExecutionContext, languageUtils: LanguageUtils)
  extends BarsLeppBaseController(identify, barsLockout, getData, checkEligibility.create(withCaching = true)) {

  def onPageLoad(): Action[AnyContent] = handle { implicit request =>
    Future.successful(
      Ok(
        view(
          leppSummary = request.leppSummary,
          backLinkUrl = Some(backLinkUrl(NormalMode, DashboardPage).url),
          continueUrl = navigator.nextPage(DashboardPage, NormalMode).url,
          barsLockFlag = request.barsLockoutExpiryOpt.nonEmpty,
          lockoutExpires = request.barsLockoutExpiryOpt
        )
      )
    )
  }
}

