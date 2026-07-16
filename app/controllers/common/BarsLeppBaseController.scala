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

package controllers.common

import controllers.actions.{BarsLockoutAction, CheckEligibilityAction, DataRetrievalAction, IdentifierAction}
import models.requests.EligibleDataRequest
import models.userAnswers.{BankAccountDetails, SubmissionSummary}
import pages.{CheckYourAnswersPage, WhatAreYourBankDetailsPage}
import play.api.mvc.{Action, AnyContent, Result}

import javax.inject.Inject
import scala.concurrent.Future

abstract class BarsLeppBaseController @Inject()(identify: IdentifierAction,
                                                barsLockout: BarsLockoutAction,
                                                getData: DataRetrievalAction,
                                                checkEligibility: CheckEligibilityAction)
  extends EligibleLeppBaseController(identify, getData, checkEligibility) {

  override def handle(f: EligibleDataRequest[AnyContent] => Future[Result]): Action[AnyContent] =
    (identify andThen barsLockout andThen getData andThen checkEligibility).async(implicit req => f(req))

  def handleWithBankDetails(f: BlockFor[BankAccountDetails]): Action[AnyContent] = handleWithSubmissionCheck { implicit req =>
    import req.userAnswers

    userAnswers.get(WhatAreYourBankDetailsPage) match {
      case Some(details) => f(req)(details)
      case _ => WhatAreYourBankDetailsPage.asFutureRedirect
    }
  }

  def handleForConfirmationPage(f: BlockFor[SubmissionSummary]): Action[AnyContent] =
    handle { implicit req =>
      import req.userAnswers
      val bankDetailsOpt: Option[BankAccountDetails] = userAnswers.get(WhatAreYourBankDetailsPage)
      val cyaSubmissionOpt: Option[SubmissionSummary] = userAnswers.get(CheckYourAnswersPage)

      (bankDetailsOpt, cyaSubmissionOpt) match {
        case (Some(_), Some(cyaSubmission)) => f(req)(cyaSubmission)
        case (None, _) => WhatAreYourBankDetailsPage.asFutureRedirect
        case (_, _) => CheckYourAnswersPage.asFutureRedirect
      }
    }
}
