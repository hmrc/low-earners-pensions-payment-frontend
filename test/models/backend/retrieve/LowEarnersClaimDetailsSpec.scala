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
import play.api.libs.json.*

class LowEarnersClaimDetailsSpec extends SpecBase {

  "LowEarnersClaimDetails reads" - {

    "convert valid JSON with all fields populated" in {
      val json = Json.obj(
        "claimSequenceNumber" -> 123,
        "entitlementAmount" -> 250.50,
        "claimStatus" -> "PAID",
        "inSelfAssessment" -> true,
        "calculationDate" -> "2026-09-25",
        "claimDate" -> "2026-09-20",
        "reminderOutputSent" -> false,
        "reissueClaimOutput" -> true,
        "originalAmount" -> 200.00
      )

      json.validate[LowEarnersClaimDetails] mustBe JsSuccess(
        LowEarnersClaimDetails(
          claimSequenceNumber = BigInt(123),
          entitlementAmount = Some(BigDecimal("250.50")),
          claimStatus = ClaimStatus.Paid,
          inSelfAssessment = true,
          calculationDate = Some("2026-09-25"),
          claimDate = Some("2026-09-20"),
          reminderOutputSent = false,
          reissueClaimOutput = true,
          originalAmount = Some(BigDecimal("200.00"))
        )
      )
    }

    "convert valid JSON with optional fields missing" in {
      val json = Json.obj(
        "claimSequenceNumber" -> 123,
        "claimStatus" -> "PENDING",
        "inSelfAssessment" -> false,
        "reminderOutputSent" -> false,
        "reissueClaimOutput" -> false
      )

      json.validate[LowEarnersClaimDetails] mustBe JsSuccess(
        LowEarnersClaimDetails(
          claimSequenceNumber = BigInt(123),
          entitlementAmount = None,
          claimStatus = ClaimStatus.Available,
          inSelfAssessment = false,
          calculationDate = None,
          claimDate = None,
          reminderOutputSent = false,
          reissueClaimOutput = false,
          originalAmount = None
        )
      )
    }

    "throw error when mandatory tracking ID, claimSequenceNumber is missing" in {
      val json = Json.obj(
        "claimStatus" -> "PENDING",
        "inSelfAssessment" -> true,
        "reminderOutputSent" -> false,
        "reissueClaimOutput" -> false
      )

      json.validate[LowEarnersClaimDetails].isError mustBe true
    }

    "throw error when no status received" in {
      val json = Json.obj(
        "claimSequenceNumber" -> 123,
        "inSelfAssessment" -> true,
        "reminderOutputSent" -> false,
        "reissueClaimOutput" -> false
      )

      json.validate[LowEarnersClaimDetails].isError mustBe true
    }

    "throw error when a field has the wrong type" in {
      val json = Json.obj(
        "claimSequenceNumber" -> 123,
        "claimStatus" -> "PENDING",
        "inSelfAssessment" -> "true",
        "reminderOutputSent" -> false,
        "reissueClaimOutput" -> false
      )

      json.validate[LowEarnersClaimDetails].isError mustBe true
    }

    "throw error when claimStatus has an invalid value" in {
      val json = Json.obj(
        "claimSequenceNumber" -> 123,
        "claimStatus" -> "NOT PENDING",
        "inSelfAssessment" -> true,
        "reminderOutputSent" -> false,
        "reissueClaimOutput" -> false
      )

      json.validate[LowEarnersClaimDetails].isError mustBe true
    }
  }
}
