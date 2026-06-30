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

package viewmodels.paymentBreakdown

import models.userAnswers.{LeppItem, LeppSummary}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import utils.CurrencyFormats

object BreakdownUtils {

  def paymentSummaryList(paymentSummary: LeppSummary, itemId: Option[String])(implicit messages: Messages): Seq[SummaryList] =
    itemId match {
      case None => paymentSummary.availableItems.getOrElse(Nil).map(availableSummaryList)
      case Some(value) => paymentSummary.paymentHistoryItems.filter(item => item.id == value).map(historySummaryList)
    }

  private[paymentBreakdown] def underPaymentSummaryListRows(item: LeppItem, originalAmount: BigDecimal)(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(SummaryListRow(
      Key(HtmlContent(messages("breakdown.underpayment.l1"))),
      Value(HtmlContent(CurrencyFormats.format(item.entitlement + originalAmount)), classes = "right-align")))
      ++
      Seq(SummaryListRow(
        Key(HtmlContent(messages("breakdown.underpayment.l2"))),
        Value(HtmlContent(CurrencyFormats.format(originalAmount)), classes = "right-align")))
      ++
      Seq(SummaryListRow(
        Key(HtmlContent(messages("breakdown.underpayment.l3"))),
        Value(HtmlContent(item.formattedEntitlement), classes = "right-align")))

  private[paymentBreakdown] def availableSummaryList(item: LeppItem)(implicit messages: Messages) = {
    val summaryListRows = item.originalAmount match {
      case Some(value) =>
        commonSummaryListRows(item) ++ underPaymentSummaryListRows(item, value)
      case None =>
        paymentSummaryListRows(item)
    }
    generateSummaryList(item, summaryListRows)
  }

  private[paymentBreakdown] def commonSummaryListRows(item: LeppItem)(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(SummaryListRow(
      Key(HtmlContent(messages("breakdown.l1"))),
      Value(HtmlContent(item.formattedContributions), classes = "right-align")))
      ++
      Seq(SummaryListRow(
        Key(HtmlContent(messages("breakdown.l2"))),
        Value(HtmlContent(item.taxRatePercent), classes = "right-align")))

  private[paymentBreakdown] def paymentSummaryListRows(item: LeppItem)(implicit messages: Messages) =
    commonSummaryListRows(item)
      ++
      Seq(SummaryListRow(
        Key(HtmlContent(messages("breakdown.l3"))),
        Value(HtmlContent(item.formattedEntitlement), classes = "right-align")))


  private[paymentBreakdown] def historySummaryList(item: LeppItem)(implicit messages: Messages) =
    generateSummaryList(item, paymentSummaryListRows(item))

  private[paymentBreakdown] def generateSummaryList(item: LeppItem, summaryListRows: Seq[SummaryListRow])(implicit messages: Messages) =
    SummaryList(
      rows = summaryListRows,
      card = Some(
        Card(
          title = Some(
            CardTitle(
              content = HtmlContent(s"${messages("breakdown.u1", item.taxYear.toString, (item.taxYear + 1).toString)}"),
              headingLevel = Some(2)
            )
          ),
          attributes = Map("id" -> item.id)
        )
      )
    )
}
