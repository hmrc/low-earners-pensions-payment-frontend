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

package viewmodels.checkYourAnswers

import controllers.routes
import forms.WhatAreYourBankDetailsFormProvider.formatSortCode
import models.userAnswers.BankAccountDetails
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{Text, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, Card, CardTitle, Key, SummaryListRow}
import viewmodels.CheckMode

object CheckYourAnswersUtils {
  private def accountNameRow(bankAccountDetails: BankAccountDetails)
                            (implicit messages: Messages): SummaryListRow = SummaryListRow(
    key = Key(content = Text(messages("bankDetails.accountName"))),
    value = Value(content = Text(bankAccountDetails.accountName))
  )

  private def accountNumberRow(bankAccountDetails: BankAccountDetails)
                              (implicit messages: Messages): SummaryListRow = SummaryListRow(
    key = Key(content = Text(messages("bankDetails.accountNumber"))),
    value = Value(content = Text(bankAccountDetails.accountNumber))
  )

  private def sortCodeRow(bankAccountDetails: BankAccountDetails)
                         (implicit messages: Messages): SummaryListRow = SummaryListRow(
    key = Key(content = Text(messages("bankDetails.sortCode"))),
    value = Value(content = Text(formatSortCode(bankAccountDetails.sortCode)))
  )

  private def rollNumberRow(bankAccountDetails: BankAccountDetails)
                           (implicit messages: Messages): Option[SummaryListRow] =
    bankAccountDetails.rollNumber.map(rollNumber =>
      SummaryListRow(
        key = Key(content = Text(messages("checkYourAnswers.rollNumber"))),
        value = Value(content = Text(rollNumber))
      )
    )
  
  def cyaSummaryListRows(accountDetails: BankAccountDetails)
                        (implicit messages: Messages): Seq[SummaryListRow] = Seq(
    accountNameRow(accountDetails),
    sortCodeRow(accountDetails),
    accountNumberRow(accountDetails)
  ) ++ Seq(rollNumberRow(accountDetails)).flatten
  
  def cyaSummaryCard()(implicit messages: Messages): Card = Card(
    title = Some(
      CardTitle(
        content = Text(messages("checkYourAnswers.bankDetails")),
        headingLevel = Some(2)
      )
    ),
    actions = Some(
      Actions(
        items = Seq(
          ActionItem(
            href = routes.WhatAreYourBankDetailsController.onPageLoad(CheckMode).url,
            content = Text(messages("site.change")),
            attributes = Map("id" -> "bankDetails_changeAction")
          )
        )
      )
    ),
    attributes = Map("id" -> "bankDetails_summaryCard")
  )
}
