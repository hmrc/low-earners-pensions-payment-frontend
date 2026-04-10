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

package models.bars

import models.bars.MatchResult.Match
import models.bars.statuses.{AccountExists, AccountNumberWellFormatted, NameMatches, NonStandardAccountDetails, SortCodeCheck}
import play.api.libs.json.{Json, Reads}

case class BarsResponse(accountNumberIsWellFormatted: AccountNumberWellFormatted,
                        accountExists: AccountExists,
                        nameMatches: NameMatches,
                        accountName: Option[String],
                        nonStandardAccountDetailsRequiredForBacs: NonStandardAccountDetails,
                        sortCodeIsPresentOnEISCD: SortCodeCheck,
                        sortCodeSupportsDirectDebit: SortCodeCheck,
                        sortCodeSupportsDirectCredit: SortCodeCheck, //
                        sortCodeBankName: Option[String],
                        iban: Option[String]) {
  def toErrorResultOpt: Option[BarsError] = None //TODO - Implement
}

object BarsResponse {
  implicit val reads: Reads[BarsResponse] = Json.reads[BarsResponse]
}
