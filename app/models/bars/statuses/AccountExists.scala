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

package models.bars.statuses

import models.bars.*
import play.api.libs.json.*

enum AccountExists(override val errorOpt: Option[BarsError] = None) extends BarsStatus {
  case Yes
  case No extends AccountExists(Some(AccountNotFoundError))
  case Inapplicable extends AccountExists(Some(FailedModulusCheckError))
  case Indeterminate extends AccountExists(Some(IndeterminateResultError("ACCOUNT_EXISTS")))
  case Error extends AccountExists(Some(ErrorsInResponseError("ACCOUNT_EXISTS")))
}

object AccountExists {
  implicit val reads: Reads[AccountExists] = Reads{
    case JsString("yes") => JsSuccess(Yes)
    case JsString("no") => JsSuccess(No)
    case JsString("inapplicable") => JsSuccess(Inapplicable)
    case JsString("indeterminate") => JsSuccess(Indeterminate)
    case JsString("error") => JsSuccess(Error)
    case _ => JsError("error.nonStandardAccountDetailsRequiredForBacs.invalid")
  }
}