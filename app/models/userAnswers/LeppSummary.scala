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

import models.backend.retrieve.RetrieveLeppDetailsResponse
import models.userAnswers.LeppItemStatus.{Available, Cancelled, Paid, Suspended}
import play.api.libs.json.{Json, OFormat}
import utils.CurrencyFormats

import java.time.LocalDate

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

  val isNonEmpty: Boolean = hasAvailablePayments || hasPaymentHistory

  private val latestClaimDateOpt: Option[LocalDate] = paidItems.flatMap(
    _
      .flatMap(_.claimDate)
      .reduceOption((date1, date2) => if (date1.isAfter(date2)) date1 else date2)
  )

  def showPaidInset(currentLocalDate: LocalDate): Boolean = latestClaimDateOpt.fold(false)(latestClaimDate => {
    latestClaimDate.plusDays(10).isAfter(currentLocalDate)
  })
}

object LeppSummary {
  def notEmptySeq[A](seq: Seq[A]): Option[Seq[A]] = if (seq.nonEmpty) Some(seq) else None

  def apply(retrieveClaimsResponse: RetrieveLeppDetailsResponse): LeppSummary = {
    import retrieveClaimsResponse.*

    val leppItems: Seq[LeppItem] = lowEarnersDetailsList.flatMap(details =>
      val taxYear = details.taxYear
      details.lowEarnersCalculations.zipWithIndex.map(
        (calc, index) => LeppItem(taxYear = taxYear, calculation = calc, index = index + 1)
      )
    )

    LeppSummary(
      currentLock = currentLowEarnersOptimisticLock,
      availableItems = notEmptySeq(leppItems.filter(_.status == Available).sortBy(_.taxYear)),
      paidItems = notEmptySeq(leppItems.filter(_.status == Paid)),
      suspendedItems = notEmptySeq(leppItems.filter(_.status == Suspended)),
      cancelledItems = notEmptySeq(leppItems.filter(_.status == Cancelled))
    )
  }

  implicit val format: OFormat[LeppSummary] = Json.format[LeppSummary]
}
