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

package controllers.actions

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import connectors.ConnectorResponse
import models.CorrelationId
import models.ResponseWrapper.ErrorWrapper
import models.requests.{DataRequest, EligibleDataRequest}
import pages.DashboardPage
import play.api.mvc.{ActionFilter, ActionRefiner, Headers, Request, Result, Results}
import services.{LeppRetrievalService, SessionCacheService}
import models.userAnswers.{LeppSummary, UserAnswers}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.{Constants, CorrelationIdHandler}
import models.errors.ErrorResult.notEligibleError

import scala.concurrent.{ExecutionContext, Future}

trait CheckEligibilityAction extends ActionRefiner[DataRequest, EligibleDataRequest] with Results

@Singleton
class AcceptPaymentCheckEligibilityAction @Inject()()(using ec: ExecutionContext) extends CheckEligibilityAction {
  override protected def executionContext: ExecutionContext = ec

  override protected[actions] def refine[A](request: DataRequest[A]): Future[Either[Result, EligibleDataRequest[A]]] = {
    val leppSummaryOpt: Option[LeppSummary] = request.userAnswers.get[LeppSummary](DashboardPage)
    
    leppSummaryOpt.fold(
      Future.successful(Left(Redirect(controllers.routes.DashboardController.onPageLoad())))
    )(leppSummary =>
      Future.successful(Right(EligibleDataRequest(request, leppSummary)))
    )
  }
    
}

@Singleton
class StartPageCheckEligibilityActionBuilder @Inject()(sessionDataService: SessionCacheService,
                                                       correlationIdHandler: CorrelationIdHandler,
                                                       leppRetrievalService: LeppRetrievalService)
                                                      (using ec: ExecutionContext) {
  def create(withCaching: Boolean): StartPageCheckEligibilityAction =
    StartPageCheckEligibilityAction(withCaching)(
      sessionDataService = sessionDataService,
      correlationIdHandler = correlationIdHandler,
      leppRetrievalService = leppRetrievalService
    )
}

@Singleton
class StartPageCheckEligibilityAction (withCaching: Boolean) 
                                      (sessionDataService: SessionCacheService,
                                       correlationIdHandler: CorrelationIdHandler,
                                       leppRetrievalService: LeppRetrievalService)
                                      (using ec: ExecutionContext)
  extends CheckEligibilityAction with Results {
  
  override protected def executionContext: ExecutionContext = ec

  override protected[actions] def refine[A](request: DataRequest[A]): Future[Either[Result, EligibleDataRequest[A]]] = {
    given hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    given cid: CorrelationId = correlationIdHandler.getCorrelationId(request)

    lazy val notEligibleRedirect: Either[Result, EligibleDataRequest[A]] = Left(
      Redirect(controllers.auth.routes.IneligibleController.onPageLoad())
    )
    
    def cachingResult(userAnswers: UserAnswers): Future[Unit] = if(withCaching){
      sessionDataService.save(userAnswers)
    } else {
     Future.successful(())
    }
    
    val result: ConnectorResponse[LeppSummary] = for {
      _ <- EitherT.right(Future.successful(sessionDataService.clear(request.userAnswers)))
      leppSummary <- leppRetrievalService.retrieveLeppDetails()
      emptyUserAnswers = UserAnswers(id = request.userAnswers.id)
      updatedUserAnswers <- EitherT.right(Future.fromTry(emptyUserAnswers.set(DashboardPage, leppSummary.value)))
      _ <- EitherT.right(cachingResult(updatedUserAnswers))
    } yield leppSummary
    
    val requestWithCid: Request[A] = ActionUtils.requestWithCid(request.request)
    val dataRequestWithCid: DataRequest[A] = request.copy(request = requestWithCid)

    result.biflatMap(
      err => EitherT(Future.successful(Left(Redirect(
        err.value match {
          case `notEligibleError` => controllers.auth.routes.IneligibleController.onPageLoad()
          case _ => controllers.routes.ClearCacheController.defaultError()
        }
      )))),
      success =>
        if(success.value.isNonEmpty)
          EitherT(Future.successful(Right(EligibleDataRequest[A](dataRequestWithCid, success.value))))
        else
          EitherT(Future.successful(notEligibleRedirect))
    ).value
  }
}
