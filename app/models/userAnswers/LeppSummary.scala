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

import models.nps.RetrieveClaimsResponse
import models.userAnswers.LeppItemStatus.{Available, Cancelled, Paid, Suspended}
import play.api.libs.json.{Json, OFormat}
import utils.CurrencyFormats

case class LeppSummary(currentLock: BigInt,
                       availableItems: Option[Seq[LeppItem]] = None,
                       paidItems: Option[Seq[LeppItem]] = None,
                       suspendedItems: Option[Seq[LeppItem]] = None,
                       cancelledItems: Option[Seq[LeppItem]] = None) {
  val availablePaymentItems: Seq[LeppItem] = Seq(availableItems, suspendedItems).flatten.flatten
  val hasAvailablePayments: Boolean = availablePaymentItems.nonEmpty

  protected[userAnswers] val totalAvailableEntitlement: BigDecimal = availableItems
    .getOrElse(Nil)
    .map(_.entitlement)
    .sum
  
  val totalEntitlementString: String = CurrencyFormats.format(totalAvailableEntitlement)
  
  val paymentHistoryItems: Seq[LeppItem] = Seq(cancelledItems, paidItems).flatten.flatten
  val hasPaymentHistory: Boolean = paymentHistoryItems.nonEmpty
}

object LeppSummary {
  def notEmptySeq[A](seq: Seq[A]): Option[Seq[A]] = if (seq.nonEmpty) Some(seq) else None

  def apply(retrieveClaimsResponse: RetrieveClaimsResponse): LeppSummary = {
    import retrieveClaimsResponse.*

    val leppItems: Seq[LeppItem] = lowEarnersDetailsList.flatMap(details =>
      val taxYear = details.taxYear
      details.lowEarnersCalculations.zipWithIndex.map(
        (calc, index) => LeppItem(taxYear = taxYear, calculation = calc, index = index + 1)
      )
    )

    LeppSummary(
      currentLock = currentLowEarnersOptimisticLock,
      availableItems = notEmptySeq(leppItems.filter(_.status == Available)),
      paidItems = notEmptySeq(leppItems.filter(_.status == Paid)),
      suspendedItems = notEmptySeq(leppItems.filter(_.status == Suspended)),
      cancelledItems = notEmptySeq(leppItems.filter(_.status == Cancelled))
    )
  }

  implicit val format: OFormat[LeppSummary] = Json.format[LeppSummary]
}
