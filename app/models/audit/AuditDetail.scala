/*
 * Copyright 2023 HM Revenue & Customs
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

package models.audit

import models.requests.AuthUser
import models.userAnswers.BankAccountDetails
import play.api.libs.json.{Json, OWrites}

case class AuditDetail(
  accountName: String,
  accountNumber: String,
  name: String,
  nino: String,
  paymentAmount: BigDecimal,
  sortCode: String,
  taxYear: String,
  paymentOutcome: PaymentOutcome
)

object AuditDetail {
  private def taxYearString(taxYear: BigInt) = s"6 April $taxYear to 5 April ${taxYear+1}"
  
  def apply(bankAccountDetails: BankAccountDetails,
            authUser: AuthUser,
            taxYear: BigInt,
            entitlement: BigDecimal,
            paymentOutcome: PaymentOutcome): AuditDetail = {
    val nameString: String = authUser.itmpNameOpt.map(name => {
      val givenName: String = name.givenName.getOrElse("")
      val middleName: String = name.middleName.getOrElse("")
      val familyName: String = name.familyName.getOrElse("")
      
      s"$givenName $middleName $familyName".replace("  ", " ")
    }).getOrElse("")
    
    AuditDetail(
      accountName = bankAccountDetails.accountName,
      accountNumber = bankAccountDetails.accountNumber,
      name = nameString,
      nino = authUser.nino.nino,
      paymentAmount = entitlement,
      sortCode = bankAccountDetails.sortCode,
      taxYear = taxYearString(taxYear),
      paymentOutcome = paymentOutcome
    )
  }
  
  
  implicit val writes: OWrites[AuditDetail] = Json.writes[AuditDetail]
}
