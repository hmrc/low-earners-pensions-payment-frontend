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
                    taxRate: BigDecimal,
                    entitlement: BigDecimal,
                    status: LeppItemStatus,
                    claimDate: Option[LocalDate],
                    originalAmount: Option[BigDecimal] = None) {
  val formattedEntitlement: String = CurrencyFormats.format(entitlement)
  val formattedContributions: String = CurrencyFormats.format(contributions)
  val taxRatePercent = s"${(taxRate * 100).intValue.toString}%"
}

object LeppItem {
  def apply(taxYear: Int, calculation: LowEarnersCalculation, index: Int): LeppItem = {
    val leppStatus: LeppItemStatus = calculation.lowEarnersClaimDetails.claimStatus.toLeppItemStatus
    val id: String = s"${leppStatus.toString.take(1)}-$taxYear-$index" 
    
    LeppItem(
      id = id,
      taxYear = taxYear,
      contributions = calculation.lowEarnersDataDetails.netPayContributionsTotal.getOrElse(0),
      taxRate = calculation.lowEarnersDataDetails.basicRatePercentage.getOrElse(0),
      entitlement = calculation.lowEarnersClaimDetails.entitlementAmount.getOrElse(0),
      status = calculation.lowEarnersClaimDetails.claimStatus.toLeppItemStatus,
      claimDate = calculation.lowEarnersClaimDetails.claimDate.map(LocalDate.parse(_)),
      originalAmount = calculation.lowEarnersClaimDetails.originalAmount
    )
  }
  
  implicit val format: OFormat[LeppItem] = Json.format[LeppItem]
}
