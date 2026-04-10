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

sealed abstract class BarsError(reason: String)

case object BarsCheckFailedError extends BarsError("ERRORS_IN_BARS_RESPONSE")
case object SortCodeNotFoundError extends BarsError("SORT_CODE_NOT_FOUND")

case object DirectCreditUnsupportedError extends BarsError("DIRECT_CREDIT_UNSUPPORTED")
case object AdditionalInfoRequiredError extends BarsError("ADDITIONAL_INFORMATION_REQUIRED")
case object NameMismatchError extends BarsError("SUPPLIED_NAME_NOT_MATCHED")
case object AccountNotFoundError extends BarsError("ACCOUNT_NOT_FOUND")
case object FailedModulusCheckError extends BarsError("FAILED_MODULUS_CHECK")
