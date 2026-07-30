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

import base.SpecBase
import models.userAnswers.LeppItem
import models.userAnswers.LeppItemStatus.Available
import play.api.Application
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.{HeadCell, TableRow}

import java.time.LocalDate

class ConfirmationUtilsSpec extends SpecBase {
  private trait Test {
    private val fakeApp: Application = applicationBuilder().build()
    implicit val fakeMessages: Messages = messages(fakeApp)
    
    val leppItem: LeppItem = LeppItem(
      id = "A-25-1",
      taxYear = 2025,
      contributions = 1000,
      taxRate = 0.2,
      entitlement = 200,
      status = Available,
      claimDate = Some(LocalDate.of(2026, 1, 1))
    )
  }
  
  "DashboardUtils" - {
    "tableHeaders" - {
      "should return the expected HeadCell" in new Test {
        val headerCells: Seq[HeadCell] = ConfirmationUtils.tableHeaders("table", Seq("taxYear")) 
        headerCells must have length 1
        headerCells.head.attributes.get("id") mustBe Some(s"table_header_taxYear")
        headerCells.head.content.asHtml.toString must include("Tax year")
      }
    }
    
    "availableTableRows" - {
      "should use claim date field when data is historic" in new Test {
        val rows: Seq[Seq[TableRow]] = ConfirmationUtils.availableTableRows(
          tableRef = "ref",
          items = Seq(leppItem)
        )
        rows must have length 1
        rows.head must have length 3
        val htmlContent: Seq[String] = rows.head.map(_.content.asHtml.toString)
        htmlContent must contain("6 April 2025 to 5 April 2026")
        htmlContent must contain("£200")
        htmlContent must contain("5 April 2030")
      }
    }
    
    "acceptedTableRows" - {
      "should default claim date field when it does not exist for historic item" in new Test {
        val rows: Seq[Seq[TableRow]] = ConfirmationUtils.acceptedTableRows(
          tableRef = "ref",
          items = Seq(leppItem.copy(claimDate = None))
        )
        rows must have length 1
        rows.head must have length 2
        val htmlContent: Seq[String] = rows.head.map(_.content.asHtml.toString)
        htmlContent must contain("6 April 2025 to 5 April 2026")
        htmlContent must contain("£200")
      }
      
      "should return available until date field when data is not historic" in new Test {
        val rows: Seq[Seq[TableRow]] = ConfirmationUtils.acceptedTableRows(
          tableRef = "ref",
          items = Seq(leppItem.copy(claimDate = None))
        )
        rows must have length 1
        rows.head must have length 2
        val htmlContent: Seq[String] = rows.head.map(_.content.asHtml.toString)
        htmlContent must contain("6 April 2025 to 5 April 2026")
        htmlContent must contain("£200")
      }
    }
  }
}
