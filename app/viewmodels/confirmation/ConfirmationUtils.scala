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

package viewmodels.confirmation

import models.userAnswers.LeppItem
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.{HeadCell, TableRow}
import viewmodels.govuk.All.TableRowViewModel

object ConfirmationUtils {
  def tableHeaders(tableRef: String, headerNames: Seq[String])
                  (implicit messages: Messages): Seq[HeadCell] = headerNames.map(headerName => {
    HeadCell(
      content = Text(messages(s"confirmation.table.header.$headerName")),
      attributes = Map("id" -> s"${tableRef}_header_$headerName")
    )
  })

  def availableTableRows(tableRef: String,
                items: Seq[LeppItem])
               (implicit messages: Messages): Seq[Seq[TableRow]] = items.map(item =>
    Seq(
      TableRowViewModel(Text(messages("common.taxYearDates", s"${item.taxYear}",
        s"${item.taxYear + 1}"))).copy(attributes = Map("id" -> s"taxYear_${item.taxYear}")),
      TableRowViewModel(Text(messages("confirmation.table.availableUntil", s"${item.taxYear + 5}")))
        .copy(attributes = Map("id" -> s"availableUntil_${item.taxYear}")),
      TableRowViewModel(Text(item.formattedEntitlement))
        .copy(attributes = Map("id" -> s"entitlement_${item.taxYear}"))
    ))

  def acceptedTableRows(tableRef: String,
                         items: Option[Seq[LeppItem]])
                        (implicit messages: Messages): Seq[Seq[TableRow]] = items.getOrElse(Nil).map {item =>
      Seq(
        TableRowViewModel(Text(messages("common.taxYearDates", s"${item.taxYear}", s"${item.taxYear + 1}")))
          .copy(attributes = Map("id" -> s"taxYear_${item.taxYear}")),
        TableRowViewModel(Text(item.formattedEntitlement))
          .copy(attributes = Map("id" -> s"entitlement_${item.taxYear}"))
      )
    }
}