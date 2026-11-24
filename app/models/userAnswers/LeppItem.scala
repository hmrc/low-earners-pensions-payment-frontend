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

import models.backend.retrieve.LowEarnersCalculation
import play.api.libs.json.*
import utils.CurrencyFormats

import java.time.LocalDate

case class LeppItem(id: String,
                    taxYear: Int,
                    contributions: BigDecimal,
                    qualifyingContributionsOpt: Option[BigDecimal] = None,
                    taxRate: BigDecimal,
                    entitlement: BigDecimal,
                    status: LeppItemStatus,
                    claimDate: Option[LocalDate],
                    originalAmount: Option[BigDecimal] = None) {
  val formattedEntitlement: String = CurrencyFormats.format(entitlement)
  val formattedContributions: String = CurrencyFormats.format(qualifyingContributionsOpt.getOrElse(contributions))
  val taxRatePercent = s"${(taxRate * 100).intValue.toString}%"
}

object LeppItem {
  def apply(taxYear: Int, calculation: LowEarnersCalculation, index: Int): LeppItem = {
    import calculation.lowEarnersClaimDetails.*
    import calculation.lowEarnersDataDetails.*
    val leppStatus: LeppItemStatus = claimStatus.toLeppItemStatus
    val id: String = s"${leppStatus.toString.take(1)}-$taxYear-$index"
    
    val calculatedQualifyingContributionsOpt = {
      val calc = entitlementAmount.getOrElse(BigDecimal(0)) / basicRatePercentage.getOrElse(BigDecimal(0))
      if (calc == netPayContributionsTotal.getOrElse(0)) None else Some(calc)
    }

    LeppItem(
      id = id,
      taxYear = taxYear,
      contributions = netPayContributionsTotal.getOrElse(0),
      qualifyingContributionsOpt = calculatedQualifyingContributionsOpt,
      taxRate = basicRatePercentage.getOrElse(0),
      entitlement = entitlementAmount.getOrElse(0),
      status = claimStatus.toLeppItemStatus,
      claimDate = claimDate.map(LocalDate.parse(_)),
      originalAmount = originalAmount
    )
  }
  
  implicit val format: OFormat[LeppItem] = Json.format[LeppItem]
}
