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

import models.userAnswers.{BankAccountDetails, LeppItem}
import play.api.libs.json.{Json, OWrites}
import uk.gov.hmrc.domain.Nino

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
  private def taxYearString(taxYear: Int) = s"6 April $taxYear to 5 April ${taxYear+1}"
  
  def apply(bankAccountDetails: BankAccountDetails,
            nino: Nino,
            leppItem: LeppItem,
            paymentOutcome: PaymentOutcome): AuditDetail = AuditDetail(
    accountName = bankAccountDetails.accountName,
    accountNumber = bankAccountDetails.accountNumber,
    name = bankAccountDetails.accountName,
    nino = nino.nino, 
    paymentAmount = leppItem.entitlement,
    sortCode = bankAccountDetails.sortCode,
    taxYear = taxYearString(leppItem.taxYear),
    paymentOutcome = paymentOutcome
  )
  
  
  implicit val writes: OWrites[AuditDetail] = Json.writes[AuditDetail]
}
