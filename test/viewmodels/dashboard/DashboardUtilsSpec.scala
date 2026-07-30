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

import base.SpecBase
import models.userAnswers.LeppItem
import models.userAnswers.LeppItemStatus.Cancelled
import play.api.Application
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.{HeadCell, TableRow}
import views.html.components.dashboard.status_element
import views.html.components.link_element

import java.time.LocalDate

class DashboardUtilsSpec extends SpecBase {
  private trait Test {
    private val fakeApp: Application = applicationBuilder().build()
    implicit val fakeMessages: Messages = messages(fakeApp)
    val statusElement: status_element = fakeApp.injector.instanceOf[status_element]
    val linkElement: link_element = fakeApp.injector.instanceOf[link_element]
    
    val leppItem: LeppItem = LeppItem(
      id = "S-25-1",
      taxYear = 2025,
      contributions = 1000,
      taxRate = 0.2,
      entitlement = 200,
      status = Cancelled,
      claimDate = Some(LocalDate.of(2026, 1, 1))
    )
  }
  
  "DashboardUtils" - {
    "tableHeaders" - {
      "should return the expected HeadCell" in new Test {
        val headerCells: Seq[HeadCell] = DashboardUtils.tableHeaders("table", Seq("taxYear")) 
        headerCells must have length 1
        headerCells.head.attributes.get("id") mustBe Some(s"table_header_taxYear")
        headerCells.head.content.asHtml.toString must include("Tax year")
      }
    }
    
    "tableRows" - {
      "should use claim date field when data is historic" in new Test {
        val rows: Seq[Seq[TableRow]] = DashboardUtils.tableRows(
          tableRef = "ref",
          items = Seq(leppItem),
          statusElementBuilder = statusElement,
          linkElementBuilderOpt = None,
          isHistoric = true
        )
        rows must have length 1
        rows.head must have length 4
        val htmlContent: Seq[String] = rows.head.map(_.content.asHtml.toString) 
        htmlContent must contain("6 April 2025 to 5 April 2026")
        htmlContent must contain("£200")
        htmlContent must contain("1 January 2026")
        htmlContent.toString() must include("Cancelled")
      }

      "should default claim date field when it does not exist for historic item" in new Test {
        val rows: Seq[Seq[TableRow]] = DashboardUtils.tableRows(
          tableRef = "ref",
          items = Seq(leppItem.copy(claimDate = None)),
          statusElementBuilder = statusElement,
          linkElementBuilderOpt = None,
          isHistoric = true
        )
        rows must have length 1
        rows.head must have length 4
        val htmlContent: Seq[String] = rows.head.map(_.content.asHtml.toString)
        htmlContent must contain("6 April 2025 to 5 April 2026")
        htmlContent must contain("£200")
        htmlContent must contain("N/A")
        htmlContent.toString() must include("Cancelled")
      }
      
      "should return available until date field when data is not historic" in new Test {
        val rows: Seq[Seq[TableRow]] = DashboardUtils.tableRows(
          tableRef = "ref",
          items = Seq(leppItem.copy(claimDate = None)),
          statusElementBuilder = statusElement,
          linkElementBuilderOpt = None
        )
        rows must have length 1
        rows.head must have length 4
        val htmlContent: Seq[String] = rows.head.map(_.content.asHtml.toString)
        htmlContent must contain("6 April 2025 to 5 April 2026")
        htmlContent must contain("£200")
        htmlContent must contain("5 April 2030")
        htmlContent.toString() must include("Cancelled")
      }
      
      "should include link field when linkElementBuilder is defined" in new Test {
        val rows: Seq[Seq[TableRow]] = DashboardUtils.tableRows(
          tableRef = "ref",
          items = Seq(leppItem),
          statusElementBuilder = statusElement,
          linkElementBuilderOpt = Some(linkElement)
        )

        rows must have length 1
        rows.head must have length 5
        val htmlContent: Seq[String] = rows.head.map(_.content.asHtml.toString)
        htmlContent must contain("6 April 2025 to 5 April 2026")
        htmlContent must contain("£200")
        htmlContent must contain("5 April 2030")
        htmlContent.toString() must include("Cancelled")
        htmlContent.toString() must include("Check calculation")
      }
    }
  }
}
