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

package models.userAnswers

import play.api.libs.json.*

enum LeppItemStatus {
  case Available, Paid, Suspended, Cancelled, Unsupported
  
  def getHtmlClass: String = this match {
    case LeppItemStatus.Available => "govuk-tag--blue"
    case LeppItemStatus.Paid => "govuk-tag--green"
    case LeppItemStatus.Suspended => "govuk-tag--yellow"
    case LeppItemStatus.Cancelled => "govuk-tag--red"
    case _ => ""
  }
  
  def toMessagesKey: String = this.toString.toLowerCase
}

object LeppItemStatus {
  implicit val reads: Reads[LeppItemStatus] = Reads {
    case JsString("Available") => JsSuccess(Available)
    case JsString("Paid") => JsSuccess(Paid)
    case JsString("Suspended") => JsSuccess(Suspended)
    case JsString("Cancelled") => JsSuccess(Cancelled)
    case _ => JsSuccess(Unsupported)
  }

  implicit val writes: Writes[LeppItemStatus] = (o: LeppItemStatus) => JsString(o.toString)
}