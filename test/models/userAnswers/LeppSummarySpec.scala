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

import base.SpecBase
import models.backend.*
import models.backend.retrieve.*
import models.backend.retrieve.ClaimStatus.{DeceasedCapacitor, Available as NpsAvailable, Cancelled as NpsCancelled, Paid as NpsPaid, Suspended as NpsSuspended}
import models.userAnswers.LeppItemStatus.{Available, Cancelled, Paid, Suspended}
import play.api.libs.json.*

import java.time.LocalDate

class LeppSummarySpec extends SpecBase {
  "LeppSummary" - {
    val model: LeppSummary = LeppSummary(
      currentLock = 67,
      availableItems = Some(Seq(
        LeppItem(
          id = "A-25-1",
          taxYear = 2025,
          contributions = 1000,
          taxRate = 20,
          entitlement = 200,
          status = Available,
          claimDate = None,
          originalAmount = None
        )
      )),
      paidItems = Some(Seq(
        LeppItem(
          id = "P-25-1",
          taxYear = 2025,
          contributions = 1000,
          taxRate = 20,
          entitlement = 200,
          status = Paid,
          claimDate = None
        )
      )),
      suspendedItems = Some(Seq(
        LeppItem(
          id = "S-25-1",
          taxYear = 2025,
          contributions = 1000,
          taxRate = 20,
          entitlement = 200,
          status = Suspended,
          claimDate = None
        )
      )),
      cancelledItems = Some(Seq(
        LeppItem(
          id = "C-25-1",
          taxYear = 2025,
          contributions = 1000,
          taxRate = 20,
          entitlement = 200,
          status = Cancelled,
          claimDate = None
        )
      )
      ))

    val json: JsValue = Json.parse(
      """
        |{
        | "currentLock": 67,
        | "availableItems": [
        |   {
        |     "id": "A-25-1",
        |     "taxYear": 2025,
        |     "contributions": 1000.00,
        |     "taxRate": 20.00,
        |     "entitlement": 200.00,
        |     "status": "Available"
        |   }
        | ],
        | "suspendedItems": [
        |   {
        |     "id": "S-25-1",
        |     "taxYear": 2025,
        |     "contributions": 1000.00,
        |     "taxRate": 20.00,
        |     "entitlement": 200.00,
        |     "status": "Suspended"
        |   }
        | ],
        | "paidItems": [
        |   {
        |     "id": "P-25-1",
        |     "taxYear": 2025,
        |     "contributions": 1000.00,
        |     "taxRate": 20.00,
        |     "entitlement": 200.00,
        |     "status": "Paid"
        |   }
        | ],
        | "cancelledItems": [
        |   {
        |     "id": "C-25-1",
        |     "taxYear": 2025,
        |     "contributions": 1000.00,
        |     "taxRate": 20.00,
        |     "entitlement": 200.00,
        |     "status": "Cancelled"
        |   }
        | ]
        |}
      """.stripMargin
    )

    "reads" - {
      "should return a JsSuccess for valid JSON" in {
        json.validate[LeppSummary] mustBe a[JsSuccess[_]]
        json.as[LeppSummary] mustBe model
      }

      "should return a JsError for invalid Json" in {
        JsObject.empty.validate[LeppSummary] mustBe a[JsError]
      }
    }

    "writes" - {
      "should return the expected JSON" in {
        Json.toJson(model) mustBe json
      }
    }
    
    "notEmptySeq" - {
      "should map empty sequence to None" in {
        LeppSummary.notEmptySeq(Nil) mustBe None
      }

      "should map non-empty sequence to optional value" in {
        LeppSummary.notEmptySeq(Seq(1)) mustBe Some(Seq(1))
      }
    }

    "apply" - {
      "should correctly construct from NPS response model" in {
        val dataDetails: LowEarnersDataDetails = LowEarnersDataDetails(
          responseTimestamp = Some("2023-06-27 09:12:28"),
          calculationSequenceNumber = 123,
          dataSourceMaster = "CESA",
          netPayContributionsTotal = Some(10.56),
          basicRatePercentage = Some(10.56),
          totalAllowances = Some(10.56),
          totalIncome = Some(10.56),
          totalDeductions = Some(10.56),
          totalTaxDue = Some(10.56)
        )

        val paidDetails: LowEarnersClaimDetails = LowEarnersClaimDetails(
          claimSequenceNumber = 123,
          entitlementAmount = Some(10.56),
          claimStatus = NpsPaid,
          inSelfAssessment = true,
          calculationDate = Some("2023-06-27"),
          claimDate = Some("2023-06-27"),
          reminderOutputSent = true,
          reissueClaimOutput = true,
          originalAmount = Some(10.56)
        )

        val paidCalculation: LowEarnersCalculation = LowEarnersCalculation(
          lowEarnersClaimDetails = paidDetails,
          lowEarnersDataDetails = dataDetails
        )

        val availableDetails: LowEarnersClaimDetails = LowEarnersClaimDetails(
          claimSequenceNumber = 123,
          entitlementAmount = Some(10.56),
          claimStatus = NpsAvailable,
          inSelfAssessment = true,
          calculationDate = Some("2023-06-27"),
          claimDate = None,
          reminderOutputSent = true,
          reissueClaimOutput = true,
          originalAmount = Some(10.56)
        )

        val availableCalculation: LowEarnersCalculation = LowEarnersCalculation(
          lowEarnersClaimDetails = availableDetails,
          lowEarnersDataDetails = dataDetails
        )

        val cancelledDetails: LowEarnersClaimDetails = LowEarnersClaimDetails(
          claimSequenceNumber = 123,
          entitlementAmount = Some(10.56),
          claimStatus = NpsCancelled,
          inSelfAssessment = true,
          calculationDate = Some("2023-06-27"),
          claimDate = None,
          reminderOutputSent = true,
          reissueClaimOutput = true,
          originalAmount = Some(10.56)
        )

        val cancelledCalculation: LowEarnersCalculation = LowEarnersCalculation(
          lowEarnersClaimDetails = cancelledDetails,
          lowEarnersDataDetails = dataDetails
        )

        val suspendedDetails: LowEarnersClaimDetails = LowEarnersClaimDetails(
          claimSequenceNumber = 123,
          entitlementAmount = Some(10.56),
          claimStatus = NpsSuspended,
          inSelfAssessment = true,
          calculationDate = Some("2023-06-27"),
          claimDate = None,
          reminderOutputSent = true,
          reissueClaimOutput = true,
          originalAmount = Some(10.56)
        )

        val suspendedCalculation: LowEarnersCalculation = LowEarnersCalculation(
          lowEarnersClaimDetails = suspendedDetails,
          lowEarnersDataDetails = dataDetails
        )

        val deceasedDetails: LowEarnersClaimDetails = LowEarnersClaimDetails(
          claimSequenceNumber = 123,
          entitlementAmount = Some(10.56),
          claimStatus = DeceasedCapacitor,
          inSelfAssessment = true,
          calculationDate = Some("2023-06-27"),
          claimDate = None,
          reminderOutputSent = true,
          reissueClaimOutput = true,
          originalAmount = Some(10.56)
        )

        val deceasedCalculation: LowEarnersCalculation = LowEarnersCalculation(
          lowEarnersClaimDetails = deceasedDetails,
          lowEarnersDataDetails = dataDetails
        )

        val details: LowEarnersDetails = LowEarnersDetails(
          taxYear = 2025,
          lowEarnersCalculations = Seq(
            availableCalculation,
            suspendedCalculation,
            cancelledCalculation,
            paidCalculation,
            deceasedCalculation
          )
        )

        val retrieveResponse: RetrieveLeppDetailsResponse = RetrieveLeppDetailsResponse(
          currentLowEarnersOptimisticLock = 123,
          identifier = "id",
          lowEarnersDetailsList = Seq(
            details,
            details.copy(taxYear = 2026)
          )
        )
        
        val leppItem = LeppItem("A-2025-1", 2025, 10.56, 10.56, 10.56, Available, None, Some(10.56))

        LeppSummary(retrieveResponse) mustBe LeppSummary(
          currentLock = 123,
          availableItems = Some(Seq(
            leppItem,
            leppItem.copy(id = "A-2026-1", taxYear = 2026)
          )),
          paidItems = Some(Seq(
            leppItem.copy(id = "P-2025-4", status = Paid, claimDate = Some(LocalDate.of(2023, 6, 27))),
            leppItem.copy(id = "P-2026-4", taxYear = 2026, status = Paid, claimDate = Some(LocalDate.of(2023, 6, 27)))
          )),
          suspendedItems = Some(Seq(
            leppItem.copy(id = "S-2025-2", status = Suspended),
            leppItem.copy(id = "S-2026-2", taxYear = 2026, status = Suspended)
          )),
          cancelledItems = Some(Seq(
            leppItem.copy(id = "C-2025-3", status = Cancelled),
            leppItem.copy(id = "C-2026-3", taxYear = 2026, status = Cancelled)
          ))
        )
      }
      
      "not include empty sequences" in {
        val leppItem = LeppItem("P-11-1", 11, 10.56, 10.56, 10.56, Paid, Some(LocalDate.of(2023, 6, 27)), Some(10.56))

        LeppSummary(retrieveResponse) mustBe LeppSummary(
          currentLock = 123,
          availableItems = None,
          paidItems = Some(Seq(leppItem)),
          suspendedItems = None,
          cancelledItems = None
        )
      } 
    }

    "availablePaymentItems" - {
      "should return an empty list when available and suspended items don't exist" in {
        LeppSummary(1).availablePaymentItems mustBe Nil
      }

      "should return a non-empty list when available and suspended items exist" in {
        model.availablePaymentItems must not be empty
      }
    }
    
    "totalAvailableEntitlement" - {
      "should return 0 when no available items exist" in {
        val model: LeppSummary = LeppSummary(
          currentLock = 67,
          availableItems = None,
          paidItems = Some(Seq(
            LeppItem(
              id = "P-25-1",
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Paid,
              claimDate = None
            )
          )),
          suspendedItems = Some(Seq(
            LeppItem(
              id = "S-25-1",
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Suspended,
              claimDate = None
            )
          )),
          cancelledItems = Some(Seq(
            LeppItem(
              id = "C-25-1",
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Cancelled,
              claimDate = None
            )
          ))
        )
        
        model.totalAvailableEntitlement mustBe 0
      }
      
      "should return correct total when available items exist" in {
        val model: LeppSummary = LeppSummary(
          currentLock = 67,
          availableItems = Some(Seq(
            LeppItem(
              id = "P-25-1",
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Paid,
              claimDate = None
            ),
            LeppItem(
              id = "P-25-1",
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Paid,
              claimDate = None
            )
          )),
          paidItems = Some(Seq(
            LeppItem(
              id = "P-25-1",
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Paid,
              claimDate = None
            )
          )),
          suspendedItems = Some(Seq(
            LeppItem(
              id = "S-25-1",
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Suspended,
              claimDate = None
            )
          )),
          cancelledItems = Some(Seq(
            LeppItem(
              id = "C-25-1",
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Cancelled,
              claimDate = None
            )
          ))
        )

        model.totalAvailableEntitlement mustBe 400
      }
    }
    
    "totalEntitlementString" - {
      "should be formatted correctly" - {
        val model: LeppSummary = LeppSummary(
          currentLock = 67,
          availableItems = Some(Seq(
            LeppItem(
              id = "P-25-1",
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Paid,
              claimDate = None
            ),
            LeppItem(
              id = "P-25-1",
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Paid,
              claimDate = None
            )
          )),
          paidItems = Some(Seq(
            LeppItem(
              id = "P-25-1",
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Paid,
              claimDate = None
            )
          )),
          suspendedItems = Some(Seq(
            LeppItem(
              id = "S-25-1",
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Suspended,
              claimDate = None
            )
          )),
          cancelledItems = Some(Seq(
            LeppItem(
              id = "C-25-1",
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Cancelled,
              claimDate = None
            )
          ))
        )
        
        model.totalEntitlementString mustBe "£400"
      }
    }

    "hasAvailablePayments" - {
      "should return true when Available or Suspended items exist" in {
        model.hasAvailablePayments mustBe true
      }


      "should return false when Available or Suspended items don't exist" in {
        val model: LeppSummary = LeppSummary(
          currentLock = 67,
          cancelledItems = Some(Seq(
            LeppItem(
              id = "C-25-1",
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Cancelled,
              claimDate = None
            )
          ))
        )

        model.hasAvailablePayments mustBe false
      }
    }

    "paymentHistoryItems" - {
      "should return an empty list when available and suspended items don't exist" in {
        LeppSummary(1).paymentHistoryItems mustBe Nil
      }

      "should return a non-empty list when available and suspended items exist" in {
        model.paymentHistoryItems must not be empty
      }
    }

    "hasPaymentHistory" - {
      "should return true when Cancelled or Paid items exist" in {
        model.hasPaymentHistory mustBe true
      }

      "should return false when Cancelled or Paid items dont exist" in {
        val model: LeppSummary = LeppSummary(
          currentLock = 67,
          suspendedItems = Some(Seq(
            LeppItem(
              id = "C-25-1",
              taxYear = 2025,
              contributions = 1000,
              taxRate = 20,
              entitlement = 200,
              status = Suspended,
              claimDate = None
            )
          ))
        )

        model.hasPaymentHistory mustBe false
      }
    }
  }
}
