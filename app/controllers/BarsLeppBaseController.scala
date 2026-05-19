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

import controllers.actions.{Actions, BarsLockoutAction, DataRetrievalAction, IdentifierAction}
import models.requests.DataRequest
import play.api.mvc.{Action, AnyContent, Result}

import javax.inject.Inject
import scala.concurrent.Future

abstract class BarsLeppBaseController @Inject()(identify: IdentifierAction,
                                                getData: DataRetrievalAction,
                                                barsLockoutAction: BarsLockoutAction)
  extends LeppBaseController(identify, getData) {

  override def handle(f: DataRequest[AnyContent] => Future[Result]): Action[AnyContent] =
    (identify andThen barsLockoutAction andThen getData).async(implicit req => f(req))  
}
