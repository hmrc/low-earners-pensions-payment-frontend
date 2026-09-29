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

class LowEarnersDataDetailsSpec extends SpecBase {

  val lowEarnersClaimDetails = LowEarnersDataDetails(
    responseTimestamp = Some("2026-09-25T15:00:00Z"),
    calculationSequenceNumber = BigInt(123),
    dataSourceMaster = "NPS",
    netPayContributionsTotal = Some(BigDecimal("100.50")),
    basicRatePercentage = Some(BigDecimal("20.00")),
    totalAllowances = Some(BigDecimal("12500.00")),
    totalIncome = Some(BigDecimal("30000.00")),
    totalDeductions = Some(BigDecimal("5000.00")),
    totalTaxDue = Some(BigDecimal("5000.00"))
  )
  
  "LowEarnersDataDetails" - {

    "convert LowEarnersDataDetails to JSON" in {

      val json = Json.toJson(lowEarnersClaimDetails)

      (json \ "responseTimestamp").as[String] mustBe "2026-09-25T15:00:00Z"
      (json \ "calculationSequenceNumber").as[BigInt] mustBe BigInt(123)
      (json \ "dataSourceMaster").as[String] mustBe "NPS"
      (json \ "netPayContributionsTotal").as[BigDecimal] mustBe BigDecimal("100.50")
      (json \ "basicRatePercentage").as[BigDecimal] mustBe BigDecimal("20.00")
    }

    "convert from JSON to LowEarnersDataDetails" in {
      val json = Json.parse(
        """
          {
            "responseTimestamp": "2026-09-25T15:00:00Z",
            "calculationSequenceNumber": 123,
            "dataSourceMaster": "NPS",
            "netPayContributionsTotal": 100.50,
            "basicRatePercentage": 20.00,
            "totalAllowances": 12500.00,
            "totalIncome": 30000.00,
            "totalDeductions": 5000.00,
            "totalTaxDue": 5000.00
          }
        """
      )

      val result = json.as[LowEarnersDataDetails]

      result mustBe lowEarnersClaimDetails
    }

    "convert with only mandatory fields" in {
      val json = Json.parse(
        """
          {
            "calculationSequenceNumber": 123,
            "dataSourceMaster": "NPS"
          }
        """
      )

      val result = json.as[LowEarnersDataDetails]

      result mustBe LowEarnersDataDetails(
        responseTimestamp = None,
        calculationSequenceNumber = BigInt(123),
        dataSourceMaster = "NPS",
        netPayContributionsTotal = None,
        basicRatePercentage = None,
        totalAllowances = None,
        totalIncome = None,
        totalDeductions = None,
        totalTaxDue = None
      )
    }

    "throw error when a required field is missing" in {
      val json = Json.parse(
        """
          {
            "dataSourceMaster": "NPS"
          }
        """
      )

      json.validate[LowEarnersDataDetails].isError mustBe true
    }
  }
}

