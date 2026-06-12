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

package models.backend.retrieve

import play.api.libs.json.{Json, Reads}

case class LowEarnersClaimDetails(claimSequenceNumber: BigInt,
                                  entitlementAmount: Option[BigDecimal],
                                  claimStatus: ClaimStatus,
                                  inSelfAssessment: Boolean,
                                  calculationDate: Option[String],
                                  claimDate: Option[String],
                                  reminderOutputSent: Boolean,
                                  reissueClaimOutput: Boolean,
                                  originalAmount: Option[BigDecimal])

object LowEarnersClaimDetails {
  implicit val reads: Reads[LowEarnersClaimDetails] = Json.reads[LowEarnersClaimDetails]
}
