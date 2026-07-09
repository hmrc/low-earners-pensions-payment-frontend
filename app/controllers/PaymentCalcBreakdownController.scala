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

import connectors.BarsVerifyStatusConnector
import controllers.actions.{AcceptPaymentCheckEligibilityAction, BarsLockoutAction, DataRetrievalAction, IdentifierAction, NoRedirectBarsLockoutAction}
import controllers.common.{BarsLeppBaseController, LeppBaseController}
import models.CorrelationId
import models.userAnswers.LeppSummary
import navigation.Navigator
import pages.*
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.CorrelationIdHandler
import viewmodels.NormalMode
import views.html.PaymentCalcBreakdownView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentCalcBreakdownController @Inject()(identify: IdentifierAction,
                                               barsLockout: NoRedirectBarsLockoutAction,
                                               getData: DataRetrievalAction,
                                               checkEligibility: AcceptPaymentCheckEligibilityAction,
                                                val controllerComponents: MessagesControllerComponents,
                                               paymentCalcBreakdownView: PaymentCalcBreakdownView,
                                               navigator: Navigator)(implicit val ec: ExecutionContext)
  extends BarsLeppBaseController(identify, barsLockout, getData, checkEligibility) {

  def onPageLoad(id: Option[String] = None): Action[AnyContent] = handleWithSubmissionCheck { implicit request =>
    Future.successful(
      Ok(
        paymentCalcBreakdownView(
          paymentSummary = request.leppSummary,
          continueUrl = navigator.nextPage(PaymentCalcBreakdownPage, NormalMode).url,
          backUrl = Some(backLinkUrl(NormalMode, PaymentCalcBreakdownPage).url),
          barsLockFlag = request.barsLockoutExpiryOpt.nonEmpty,
          itemId = id
        )
      ) 
    )
  }
}
  