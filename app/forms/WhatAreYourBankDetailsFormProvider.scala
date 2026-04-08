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

package forms

import models.userAnswers.BankAccountDetails
import play.api.data.Form
import play.api.data.Forms.mapping

class WhatAreYourBankDetailsFormProvider extends BaseForm {
  private[forms] def stripSortCode(str: String): String = str.strip().replaceAll("(?<=[0-9])-(?=[0-9])", "")
  private[forms] def formatSortCode(str: String): String = s"${str.take(2)}-${str.slice(2, 4)}-${str.drop(4)}"
  private[forms] def formatAccountName(str: String): String = str.toLowerCase.split(" ").map(_.capitalize).mkString(" ")

  private val bankDetails = "bankDetails"

  def apply(): Form[BankAccountDetails] = Form[BankAccountDetails](
    mapping(
      mandatoryTextField(s"$bankDetails.accountName", 1, 18, "^[0-9A-Za-z'&,\\\\=()\\/ -]+$"),
      mandatoryTextField(s"$bankDetails.accountNumber", 6, 8, "^[0-9]{6,8}$", unbindMap = formatAccountName),
      mandatoryTextField(s"$bankDetails.sortCode", 6, 6, "^[0-9]{6}$", stripSortCode, formatSortCode),
      optionalTextField(s"$bankDetails.rollNumber", 1, 18, "^[A-Z0-9]{1,18}$")
    )(BankAccountDetails.apply)(BankAccountDetails.unapply)
  )
}
