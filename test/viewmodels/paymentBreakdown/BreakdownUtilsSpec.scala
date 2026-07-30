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

import base.SpecBase
import models.userAnswers.LeppItemStatus.{Available, Paid}
import models.userAnswers.{LeppItem, LeppSummary}
import play.api.Application
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}

import java.time.LocalDate

class BreakdownUtilsSpec extends SpecBase {
  
  val taxYear = 2025
  
  val leppItem: LeppItem = LeppItem(
    id = "S-25-1",
    taxYear = taxYear,
    contributions = 1000,
    taxRate = 20,
    entitlement = 200,
    status = Available,
    claimDate = Some(LocalDate.of(2026, 1, 1))
  )

  val historyLeppItem: LeppItem = LeppItem(
    id = "P-25-1",
    taxYear = taxYear,
    contributions = 1000,
    taxRate = 20,
    entitlement = 200,
    status = Paid,
    claimDate = Some(LocalDate.of(2026, 1, 1))
  )
  
  private trait Test {
    private val fakeApp: Application = applicationBuilder().build()
    implicit val fakeMessages: Messages = messages(fakeApp)
    
    val leppSummary = LeppSummary(currentLock = 1,
      availableItems = Some(Seq(leppItem)),
      paidItems = Some(Seq(historyLeppItem)))
  }

  private def commonAssertions(summaryListRows: Seq[SummaryListRow])(implicit fakeMessages: Messages): Unit = {
    summaryListRows.head.key.content.asHtml.toString mustBe fakeMessages("breakdown.l1")
    summaryListRows.head.value.content.asHtml.toString mustBe "£1000"
    summaryListRows(1).key.content.asHtml.toString mustBe fakeMessages("breakdown.l2")
    summaryListRows(1).value.content.asHtml.toString mustBe "20%"
  }

  private def normalPaymentAssertions(summaryListRows: Seq[SummaryListRow])(implicit fakeMessages: Messages): Unit = {
    commonAssertions(summaryListRows)
    summaryListRows(2).key.content.asHtml.toString mustBe fakeMessages("breakdown.l3")
    summaryListRows(2).value.content.asHtml.toString mustBe "£200"
  }

  private def summaryListCardAssertions(summaryList: SummaryList)(implicit fakeMessages: Messages): Unit = {
    summaryList.card.isDefined mustBe true
    summaryList.card.get.title.isDefined mustBe true
    summaryList.card.get.title.get.content.asHtml.toString mustBe
      fakeMessages("breakdown.u1", taxYear.toString, (taxYear + 1).toString)
  }
  
  "BreakdownUtils" - {
    "commonSummaryListRows" - {
      "should return the expected SummaryListRows" in new Test {
        val summaryListRows: Seq[SummaryListRow] = BreakdownUtils.commonSummaryListRows(leppItem)
        summaryListRows must have length 2
        commonAssertions(summaryListRows)
      }
    }

    "paymentSummaryListRows" - {
      "should return the expected SummaryListRows" in new Test {
        val summaryListRows: Seq[SummaryListRow] = BreakdownUtils.paymentSummaryListRows(leppItem)
        summaryListRows must have length 3
        normalPaymentAssertions(summaryListRows)
      }
    }

    "underPaymentSummaryListRows" - {
      "should return the expected SummaryListRows" in new Test {
        val originalAmount = 100
        val underPaymentItem: LeppItem = leppItem.copy(originalAmount = Some(originalAmount))
        val summaryListRows: Seq[SummaryListRow] = BreakdownUtils.underPaymentSummaryListRows(underPaymentItem, originalAmount)
        summaryListRows must have length 3
        summaryListRows.head.key.content.asHtml.toString mustBe
          fakeMessages("breakdown.underpayment.l1")
        summaryListRows.head.value.content.asHtml.toString mustBe "£300"
        summaryListRows(1).key.content.asHtml.toString mustBe fakeMessages("breakdown.underpayment.l2")
        summaryListRows(1).value.content.asHtml.toString mustBe "£100"
        summaryListRows(2).key.content.asHtml.toString mustBe fakeMessages("breakdown.underpayment.l3")
        summaryListRows(2).value.content.asHtml.toString mustBe "£200"
      }
    }

    "availableSummaryList" - {
      "should return the expected SummaryList for a normal payment" in new Test {
        val summaryList: SummaryList = BreakdownUtils.breakdownSummaryList(leppItem)
        val summaryListRows: Seq[SummaryListRow] = summaryList.rows
        normalPaymentAssertions(summaryListRows)
        summaryListCardAssertions(summaryList)
        summaryList.card.get.attributes.get("id") mustBe Some(leppItem.id)
      }
      
      "should return the expected SummaryList for a under payment" in new Test {
        val originalAmount = 100
        val underPaymentItem: LeppItem = leppItem.copy(originalAmount = Some(originalAmount))
        val summaryList: SummaryList = BreakdownUtils.breakdownSummaryList(underPaymentItem)
        val summaryListRows: Seq[SummaryListRow] = summaryList.rows
        commonAssertions(summaryListRows)
        summaryListRows(2).key.content.asHtml.toString mustBe
          fakeMessages("breakdown.underpayment.l1")
        summaryListRows(2).value.content.asHtml.toString mustBe "£300"
        summaryListRows(3).key.content.asHtml.toString mustBe fakeMessages("breakdown.underpayment.l2")
        summaryListRows(3).value.content.asHtml.toString mustBe "£100"
        summaryListRows(4).key.content.asHtml.toString mustBe fakeMessages("breakdown.underpayment.l3")
        summaryListRows(4).value.content.asHtml.toString mustBe "£200"
        summaryListCardAssertions(summaryList)
        summaryList.card.get.attributes.get("id") mustBe Some(leppItem.id)
      }
    }

    "paymentSummaryList" - {
      "should return the expected SummaryLists for AVAILABLE section" in new Test {
        val summaryLists: Seq[SummaryList] = BreakdownUtils.paymentSummaryList(leppSummary, None)
        val summaryListHead: SummaryList = summaryLists.head
        val summaryListRows: Seq[SummaryListRow] = summaryListHead.rows
        normalPaymentAssertions(summaryListRows)
        summaryListCardAssertions(summaryListHead)
        summaryListHead.card.get.attributes.get("id") mustBe Some(leppItem.id)
      }

      "should return the expected SummaryLists for HISTORY section" in new Test {
        val summaryLists: Seq[SummaryList] = BreakdownUtils.paymentSummaryList(leppSummary, Some(historyLeppItem.id))
        val summaryListHead: SummaryList = summaryLists.head
        val summaryListRows: Seq[SummaryListRow] = summaryListHead.rows
        normalPaymentAssertions(summaryListRows)
        summaryListCardAssertions(summaryListHead)
        summaryListHead.card.get.attributes.get("id") mustBe Some(historyLeppItem.id)
      }
    }    
  }
}
