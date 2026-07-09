package controllers.actions.fakes

import controllers.actions.{StartPageCheckEligibilityActionBuilder, StartPageCheckEligibilityAction}
import models.requests.{DataRequest, EligibleDataRequest}
import models.userAnswers.LeppSummary
import play.api.mvc.Result
import services.SessionCacheService
import utils.CorrelationIdHandler
import services.LeppRetrievalService

import scala.concurrent.{ExecutionContext, Future}

class FakeStartPageCheckEligibilityActionBuilder(result: Either[Unit, LeppSummary])
                                                (using cacheService: SessionCacheService,
                                                 cidHandler: CorrelationIdHandler,
                                                 retrievalService: LeppRetrievalService)
                                                (using ExecutionContext)
  extends StartPageCheckEligibilityActionBuilder(cacheService, cidHandler, retrievalService){

  override def create(withCaching: Boolean): StartPageCheckEligibilityAction = new FakeStartPageCheckEligibilityAction(
    result = result
  )
}

class FakeStartPageCheckEligibilityAction(result: Either[Unit, LeppSummary])
                                         (using cacheService: SessionCacheService,
                                          cidHandler: CorrelationIdHandler,
                                          retrievalService: LeppRetrievalService)
                                         (using ExecutionContext)
  extends StartPageCheckEligibilityAction(withCaching = false)(
    sessionDataService = cacheService,
    correlationIdHandler = cidHandler,
    leppRetrievalService = retrievalService
  ) {

  override protected[actions] def refine[A](request: DataRequest[A]): Future[Either[Result, EligibleDataRequest[A]]] =
    Future.successful(
      result match {
        case Left(_) => Left(Redirect(controllers.auth.routes.IneligibleController.onPageLoad()))
        case Right(leppSummary) => Right(EligibleDataRequest(request, leppSummary = leppSummary))
      }

    )
}
