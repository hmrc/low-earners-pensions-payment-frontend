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
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, Key, SummaryListRow}
import viewmodels.CheckMode

object CheckYourAnswersSummary {
  private def accountNameRow(bankAccountDetails: BankAccountDetails)
                            (implicit messages: Messages): SummaryListRow = SummaryListRow(
    key = Key(content = Text(messages("bankDetails.accountName"))),
    value = Value(content = Text(bankAccountDetails.accountName)),
    actions = Some(Actions(
      items = Seq(ActionItem(
        href = routes.WhatAreYourBankDetailsController.onPageLoad(CheckMode).url + "#bankDetails.accountName",
        content = Text(messages("site.change")),
        visuallyHiddenText = Some(messages("checkYourAnswers.hiddenText.accountName")),
        attributes = Map("id" -> messages("checkYourAnswers.changeAccountName"))
      ))
    ))
  )

  private def accountNumberRow(bankAccountDetails: BankAccountDetails)
                              (implicit messages: Messages): SummaryListRow = SummaryListRow(
    key = Key(content = Text(messages("bankDetails.accountNumber"))),
    value = Value(content = Text(bankAccountDetails.accountNumber)),
    actions = Some(Actions(
      items = Seq(ActionItem(
        href = routes.WhatAreYourBankDetailsController.onPageLoad(CheckMode).url + "#bankDetails.accountNumber",
        content = Text(messages("site.change")),
        visuallyHiddenText = Some(messages("checkYourAnswers.hiddenText.accountNumber")),
        attributes = Map("id" -> messages("checkYourAnswers.changeAccountNumber"))
      ))
    ))
  )

  private def sortCodeRow(bankAccountDetails: BankAccountDetails)
                         (implicit messages: Messages): SummaryListRow = SummaryListRow(
    key = Key(content = Text(messages("bankDetails.sortCode"))),
    value = Value(content = Text(formatSortCode(bankAccountDetails.sortCode))),
    actions = Some(Actions(
      items = Seq(ActionItem(
        href = routes.WhatAreYourBankDetailsController.onPageLoad(CheckMode).url + "#bankDetails.sortCode",
        content = Text(messages("site.change")),
        visuallyHiddenText = Some(messages("checkYourAnswers.hiddenText.sortCode")),
        attributes = Map("id" -> messages("checkYourAnswers.changeSortCode"))
      ))
    ))
  )

  private def rollNumberRow(bankAccountDetails: BankAccountDetails)
                           (implicit messages: Messages): Option[SummaryListRow] =
    bankAccountDetails.rollNumber.map(rollNumber =>
      SummaryListRow(
        key = Key(content = Text(messages("bankDetails.rollNumber"))),
        value = Value(content = Text(rollNumber)),
        actions = Some(
          Actions(
            items = Seq(
              ActionItem(
                href = routes.WhatAreYourBankDetailsController.onPageLoad(CheckMode).url + "#bankDetails.rollNumber",
                content = Text(messages("site.change")),
                visuallyHiddenText = Some(messages("checkYourAnswers.hiddenText.rollNumber")),
                attributes = Map("id" -> messages("checkYourAnswers.changeRollNumber"))
              )
            )
          )
        )
      )
    )
  
  def cyaSummaryList(accountDetails: BankAccountDetails)
                    (implicit messages: Messages): Seq[SummaryListRow] = Seq(
    accountNameRow(accountDetails),
    accountNumberRow(accountDetails),
    sortCodeRow(accountDetails)
  ) ++ Seq(rollNumberRow(accountDetails)).flatten
}
