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
import controllers.actions.{BarsLockoutAction, DataRetrievalAction, IdentifierAction}
import models.ResponseWrapper.ErrorWrapper
import models.userAnswers.BankAccountDetails
import models.{CorrelationId, ResponseWrapper}
import navigation.Navigator
import pages.CheckYourAnswersPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{BarsService, LeppSubmissionService, SessionCacheService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{CorrelationIdHandler, Logging, MethodContext}
import viewmodels.NormalMode
import viewmodels.checkYourAnswers.CheckYourAnswersSummary.cyaSummaryList
import views.html.CheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckYourAnswersController @Inject()(identify: IdentifierAction,
                                           barsLockout: BarsLockoutAction,
                                           getData: DataRetrievalAction,
                                           view: CheckYourAnswersView,
                                           correlationIdHandler: CorrelationIdHandler,
                                           barsService: BarsService,
                                           leppSubmissionService: LeppSubmissionService,
                                           navigator: Navigator,
                                           val sessionService: SessionCacheService,
                                           barsVerifyStatusConnector: BarsVerifyStatusConnector,
                                           val controllerComponents: MessagesControllerComponents)
                                          (implicit val ec: ExecutionContext)
  extends BarsLeppBaseController(identify, getData, barsLockout) with I18nSupport with SessionDataHandling with Logging {

  def onPageLoad(): Action[AnyContent] = handleWithBankDetails { implicit req =>
    (_, bankDetails) =>
      Future.successful(Ok(
        view(
          summaryList = cyaSummaryList(bankDetails),
          viewModel = viewModel(NormalMode, CheckYourAnswersPage)
        )
      ))
  }

  def onSubmit(): Action[AnyContent] = handleWithBankDetails { implicit req =>
    (leppData, bankDetails) =>
      implicit val correlationId: CorrelationId = correlationIdHandler.getCorrelationId(req)
      
      handleWithBars(bankDetails)(() => {
        val result = for {
          leppSummary <- leppSubmissionService.acceptMultiplePayments(req.user.nino, leppData, bankDetails)
          updatedAnswers <- EitherT.right(Future.fromTry(req.userAnswers.set(CheckYourAnswersPage, leppSummary.value)))
          _ <- EitherT.right(sessionService.save(updatedAnswers))
        } yield Redirect(navigator.nextPage(CheckYourAnswersPage, NormalMode))
        
        result.leftMap(_ => Redirect(routes.ClearCacheController.defaultError())).merge
      })
  }

  protected[controllers] def handleWithBars(bankDetails: BankAccountDetails)(f: () => Future[Result])
                                           (implicit hc: HeaderCarrier,
                                            ec: ExecutionContext,
                                            cid: CorrelationId): Future[Result] = {
    given mc: MethodContext = MethodContext("handleWithBars")
    
    barsService
      .checkBankAccountDetails(bankDetails.toBarsRequest)
      .leftMap {
        case ErrorWrapper(err, _) if err.code == "BARS_REQUEST_ERRORS" =>
          barsVerifyStatusConnector
            .update().map { _ =>
              logger.info(s"Bars VerifyStatus update successful for correlationId : $cid" 
              )
            } recover { case e =>
            logger.error(s"Bars VerifyStatus update failed for: correlationId : $cid"
            )
          }
          Redirect(bars.routes.BarsRequestErrorsController.onPageLoad())
        case err =>
          Redirect(bars.routes.BarsCheckFailedController.onPageLoad())
      }
      .semiflatMap(_ => f())
      .merge
  }
}
