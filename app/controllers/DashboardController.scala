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
import controllers.actions.{DataRetrievalAction, IdentifierAction}
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
                                    getData: DataRetrievalAction,
                                    barsVerifyStatusConnector: BarsVerifyStatusConnector,
                                    correlationIdHandler: CorrelationIdHandler,
                                    leppRetrievalService: LeppRetrievalService,
                                    val sessionService: SessionCacheService,
                                    val controllerComponents: MessagesControllerComponents,
                                    view: DashboardView,
                                    navigator: Navigator)
                                   (implicit val ec: ExecutionContext, languageUtils: LanguageUtils)
  extends LeppBaseController(identify, getData) with I18nSupport with SessionDataHandling {

  def onPageLoad(): Action[AnyContent] = handleWithSubmissionCheck { implicit request =>
    implicit val cid: CorrelationId = correlationIdHandler.getCorrelationId(request)

    val result: EitherT[Future, ResponseWrapper.ErrorWrapper, Result] = for {
      leppSummary <- leppRetrievalService.retrieveLeppDetails()
      updatedAnswers <- EitherT.right(Future.fromTry(request.userAnswers.set(DashboardPage, leppSummary.value)))
      _ <- EitherT.right(sessionService.save(updatedAnswers))
      barsStatus <- EitherT.right(barsVerifyStatusConnector.status())
    } yield {
      Ok(view(
        leppSummary = leppSummary.value,
        backLinkUrl = Some(backLinkUrl(NormalMode, DashboardPage).url),
        continueUrl = navigator.nextPage(DashboardPage, NormalMode).url,
        barsLockFlag = barsStatus.lockoutExpiryDateTime.nonEmpty,
        lockoutExpires = barsStatus.lockoutExpiryDateTime
      ))
    }

    result.leftMap(mapErrors).merge
  }

  private def mapErrors[A](err: ErrorWrapper): Result = {
    err.value match {
      case `notEligibleError` => Redirect(controllers.auth.routes.IneligibleController.onPageLoad())
      case _ => Redirect(controllers.routes.SomethingWentWrongController.onPageLoad())
    }
  }
}

