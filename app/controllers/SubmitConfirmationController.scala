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

import controllers.actions.{AcceptPaymentCheckEligibilityAction, DataRetrievalAction, IdentifierAction, RedirectBarsLockoutAction}
import controllers.common.BarsLeppBaseController
import models.userAnswers.LeppItem
import pages.SubmissionPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SessionCacheService
import utils.{DateTime, DateTimeFormats}
import views.html.SubmitConfirmationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmitConfirmationController @Inject()(identify: IdentifierAction,
                                             barsLockout: RedirectBarsLockoutAction,
                                             getData: DataRetrievalAction,
                                             checkEligibility: AcceptPaymentCheckEligibilityAction,
                                             val controllerComponents: MessagesControllerComponents,
                                             confirmationView: SubmitConfirmationView,
                                             dateTime: DateTime,
                                             val sessionService: SessionCacheService)
                                            (implicit val ec: ExecutionContext)
  extends BarsLeppBaseController(identify, barsLockout, getData, checkEligibility):

  private def filterForIds(items: Option[Seq[LeppItem]], ids: Seq[String]): Seq[LeppItem] = 
    items.getOrElse(Nil).filter(item => ids.contains(item.id))
  
  def onPageLoad(): Action[AnyContent] = handleForConfirmationPage { implicit request =>
    submissionSummary =>
      val submissionDate = request.userAnswers.get(SubmissionPage) match {
        case Some(date) => date
        case _ => DateTimeFormats.getCurrentDateTimestamp(dateTime.now())
      }
      
      import request.leppSummary.availableItems
      val acceptedItems: Seq[LeppItem] = filterForIds(availableItems, submissionSummary.acceptedIds)
      val notAcceptedItems: Seq[LeppItem] = filterForIds(availableItems, submissionSummary.notAcceptedIds)
      
      if (acceptedItems.nonEmpty) {
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(SubmissionPage, submissionDate))
          _ <- sessionService.save(updatedAnswers)
        } yield
          Ok(confirmationView(
            acceptedItems = acceptedItems,
            notAcceptedItems = notAcceptedItems,
            formattedTimestamp = submissionDate
          ))
      } else
        Future.successful(Redirect(routes.ClearCacheController.defaultError()))
  
}
  
    