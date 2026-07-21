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

import controllers.actions.{CheckEligibilityAction, DataRetrievalAction, IdentifierAction}
import controllers.routes
import models.requests.EligibleDataRequest
import pages.*
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.Future

abstract class EligibleLeppBaseController @Inject()(identify: IdentifierAction,
                                                    getData: DataRetrievalAction,
                                                    checkEligibility: CheckEligibilityAction)
  extends FrontendBaseController with UrlSupport with I18nSupport {
  
  protected[controllers] type BlockFor[A] = EligibleDataRequest[AnyContent] => A => Future[Result]
  
  protected[controllers] def handle(f: EligibleDataRequest[AnyContent] => Future[Result]): Action[AnyContent] =
    (identify andThen getData andThen checkEligibility).async(implicit req => f(req))

  protected[controllers] def handleWithSubmissionCheck(f: EligibleDataRequest[AnyContent] => Future[Result]): Action[AnyContent] =
    handle { implicit req =>
      req.userAnswers.get(SubmissionPage) match {
        case Some(_) => Future.successful(Redirect(routes.ClearCacheController.onPageLoad()))
        case _ => f(req)
      }
    }
}
