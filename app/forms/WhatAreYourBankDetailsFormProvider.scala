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

import forms.WhatAreYourBankDetailsFormProvider.formatSortCode
import models.userAnswers.BankAccountDetails
import play.api.data.Forms.mapping
import play.api.data.{Form, Mapping}

class WhatAreYourBankDetailsFormProvider extends BaseForm {
  private val prefix = "bankDetails"

  private[forms] def accountNameBindMap(str: String): String =
    stripExcessWhitespace(str)
      .toLowerCase
      .split(" ")
      .map(_.capitalize)
      .mkString(" ")

  private val accountNameMapping: (String, Mapping[String]) = mandatoryTextField(
    fieldName = s"$prefix.accountName", 
    minAcceptedLength = 1, 
    maxAcceptedLength = 18, 
    regex = "^[0-9A-Za-z'&,\\\\=()\\/ -]+$",
    bindMap = accountNameBindMap
  )

  private[forms] def sortCodeBindMap(str: String): String =
    stripAllWhitespace(str)
      .replaceAll("(?<=[0-9]{2})-(?=[0-9]{2})", "")

  private val sortCodeMapping = mandatoryTextField(
    fieldName = s"$prefix.sortCode",
    minAcceptedLength = 6, maxAcceptedLength = 6, regex = "^[0-9]{6}$",
    bindMap = sortCodeBindMap,
    unbindMap = formatSortCode
  )
  
  private val accountNumberMapping = mandatoryTextField(
    fieldName = s"$prefix.accountNumber", 
    minAcceptedLength = 6, 
    maxAcceptedLength = 8, 
    regex = "^[0-9]{6,8}$"
  )

  private[forms] def rollNumberBindMap(str: Option[String]): Option[String] = str.map(_.replaceAll("[- /.]", ""))

  private val rollNumberMapping: (String, Mapping[Option[String]]) = optionalTextField(
    fieldName = s"$prefix.rollNumber",
    minAcceptedLength = 1, 
    maxAcceptedLength = 18, 
    regex = "^[A-Z0-9- /.]{1,18}$",
    bindMap = rollNumberBindMap
  )

  def apply(): Form[BankAccountDetails] = Form[BankAccountDetails](
    mapping(
      accountNameMapping,
      sortCodeMapping,
      accountNumberMapping,
      rollNumberMapping
    )(BankAccountDetails.apply)(BankAccountDetails.unapply)
  )
}

object WhatAreYourBankDetailsFormProvider {
  def formatSortCode(str: String): String = s"${str.take(2)}-${str.slice(2, 4)}-${str.drop(4)}"
}
