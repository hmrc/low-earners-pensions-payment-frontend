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

package viewmodels.dashboard

import models.userAnswers.LeppItem
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.{HeadCell, TableRow}
import viewmodels.govuk.All.TableRowViewModel
import views.html.components.dashboard.status_element
import views.html.components.link_element

object DashboardUtils {
  def tableHeaders(tableRef: String, headerNames: Seq[String])
                  (implicit messages: Messages): Seq[HeadCell] = headerNames.map(headerName => {
    HeadCell(
      content = Text(messages(s"dashboard.table.header.$headerName")),
      attributes = Map("id" -> s"${tableRef}_header_$headerName")
    ) 
  })
  
  def tableRows(tableRef: String,
                items: Seq[LeppItem],
                statusElementBuilder: status_element,
                linkElementBuilderOpt: Option[link_element] = None,
                isHistoric: Boolean = false)
               (implicit messages: Messages): Seq[Seq[TableRow]] = items.map(item => {
    val dateRow: TableRow = if (isHistoric){
      TableRowViewModel(Text(
        item.claimDate
          .map(date => s"${date.getDayOfMonth} ${messages(s"month.${date.getMonthValue}")} ${date.getYear}")
          .getOrElse("N/A"))
      )
    } else {
      TableRowViewModel(Text(messages("dashboard.table.availableUntil", s"${item.taxYear + 5}")))
    }
    
    val baseRows: Seq[TableRow] = Seq(
      TableRowViewModel(Text(messages("common.taxYearDates", s"${item.taxYear}", s"${item.taxYear + 1}"))),
      TableRowViewModel(Text(item.formattedEntitlement)),
      dateRow,
      TableRowViewModel(statusElementBuilder(status = item.status))
    )
    
    linkElementBuilderOpt.fold(baseRows)(linkElementBuilder => 
      val linkRow: TableRow = TableRowViewModel(
        linkElementBuilder(
          href = s"${controllers.routes.PaymentCalcBreakdownController.onPageLoad(Some(item.id))}",
          msgKey = "dashboard.table.link.checkCalculation"
        )
      )

      baseRows :+ linkRow
    )
  })
  
}
