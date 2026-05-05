/*
 * Copyright 2023 HM Revenue & Customs
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

import controllers.actions.request.{BarsVerifiedRequest, LockedOutJourneyRequest}
import models.requests.IdentifierRequest
import play.api.mvc.{ActionBuilder, AnyContent, DefaultActionBuilder, Request}

import javax.inject.Inject

class Actions @Inject()(
  actionBuilder:                     DefaultActionBuilder,
  authorisedRefiner:                 IdentifierAction,
  barsLockoutActionRefiner:          BarsLockoutActionRefiner,
  barsLockedOutJourneyActionRefiner: BarsLockedOutJourneyActionRefiner
) {

  val default: ActionBuilder[Request, AnyContent] = actionBuilder

  val authenticatedAction: ActionBuilder[BarsVerifiedRequest, AnyContent] =
    actionBuilder
      .andThen[IdentifierRequest](authorisedRefiner)
      .andThen[BarsVerifiedRequest](barsLockoutActionRefiner)

  val barsLockedOutAction: ActionBuilder[LockedOutJourneyRequest, AnyContent] =
    actionBuilder
      .andThen[IdentifierRequest](authorisedRefiner)
      .andThen[LockedOutJourneyRequest](barsLockedOutJourneyActionRefiner)
}
