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

package models.nps

import play.api.libs.json.*

enum ClaimStatus {
  case Available, Paid, Suspended, Cancelled, AvailableCapacitor, DeceasedCapacitor, DeceasedNoCapacitor
}

object ClaimStatus {
  implicit val reads: Reads[ClaimStatus] = Reads[ClaimStatus] {
    case JsString("CANCELLED") => JsSuccess(Cancelled)
    case JsString("DECEASED - CAPACITOR") => JsSuccess(DeceasedCapacitor)
    case JsString("DECEASED - NO CAPACITOR") => JsSuccess(DeceasedNoCapacitor)
    case JsString("PAID") => JsSuccess(Paid)
    case JsString("PENDING") => JsSuccess(Available)
    case JsString("PENDING - CAPACITOR") => JsSuccess(AvailableCapacitor)
    case JsString("SUSPENDED - RLS") => JsSuccess(Suspended)
    case _ => JsError("error.claimStatus.invalid")
  }
  
  implicit val writes: Writes[ClaimStatus] = (o: ClaimStatus) => JsString(o.toString)
}