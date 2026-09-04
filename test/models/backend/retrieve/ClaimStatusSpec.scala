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

package models.backend.retrieve

import base.SpecBase
import models.backend.retrieve.ClaimStatus.*
import play.api.libs.json.{JsString, JsSuccess, JsError, JsNumber}

class ClaimStatusSpec extends SpecBase {

  "ClaimStatus reads" - {

    "CANCELLED as Cancelled" in {
      JsString("CANCELLED").validate[ClaimStatus] mustBe JsSuccess(Cancelled)
    }

    "DECEASED - CAPACITOR as DeceasedCapacitor" in {
      JsString("DECEASED - CAPACITOR").validate[ClaimStatus] mustBe JsSuccess(DeceasedCapacitor)
    }

    "DECEASED - NO CAPACITOR as DeceasedNoCapacitor" in {
      JsString("DECEASED - NO CAPACITOR").validate[ClaimStatus] mustBe JsSuccess(DeceasedNoCapacitor)
    }

    "PAID as Paid" in {
      JsString("PAID").validate[ClaimStatus] mustBe JsSuccess(Paid)
    }

    "PENDING as Available" in {
      JsString("PENDING").validate[ClaimStatus] mustBe JsSuccess(Available)
    }

    "PENDING - CAPACITOR as Available" in {
      JsString("PENDING - CAPACITOR").validate[ClaimStatus] mustBe JsSuccess(Available)
    }

    "SUSPENDED - RLS as Suspended" in {
      JsString("SUSPENDED - RLS").validate[ClaimStatus] mustBe JsSuccess(Suspended)
    }

    "return an error for an unknown status" in {
      JsString("UNKNOWN").validate[ClaimStatus] mustBe
        JsError("error.claimStatus.invalid")
    }

    "return an error for non-string JSON" in {
      JsNumber(123).validate[ClaimStatus] mustBe
        JsError("error.claimStatus.invalid")
    }
  }
}
