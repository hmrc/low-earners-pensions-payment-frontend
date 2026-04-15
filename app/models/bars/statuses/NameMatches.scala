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

enum NameMatches(override val errorOpt: Option[BarsError] = None) extends BarsStatus {
  case Yes, Partial
  case No extends NameMatches(Some(NameMismatchError))
  case Inapplicable extends NameMatches(Some(FailedModulusCheckError))
  case Indeterminate extends NameMatches(Some(IndeterminateResultError))
  case Error extends NameMatches(Some(BarsCheckFailedError))
}

object NameMatches {
  implicit val reads: Reads[NameMatches] = Reads{
    case JsString("yes") => JsSuccess(Yes)
    case JsString("no") => JsSuccess(No)
    case JsString("partial") => JsSuccess(Partial)
    case JsString("inapplicable") => JsSuccess(Inapplicable)
    case JsString("indeterminate") => JsSuccess(Indeterminate)
    case JsString("error") => JsSuccess(Error)
    case _ => JsError("error.nonStandardAccountDetailsRequiredForBacs.invalid")
  }
}