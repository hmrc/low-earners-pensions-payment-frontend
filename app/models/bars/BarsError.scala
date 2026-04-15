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

sealed abstract class BarsError(val reason: String)
sealed abstract class BarsFailedCheckError(override val reason: String) extends BarsError(reason)
sealed abstract class BarsRequestError(override val reason: String) extends BarsError(reason)

case object SortCodeNotFoundError extends BarsRequestError("SORT_CODE_NOT_FOUND")
case object AccountNotFoundError extends BarsRequestError("ACCOUNT_NOT_FOUND")
case object NameMismatchError extends BarsRequestError("SUPPLIED_NAME_NOT_MATCHED")
case object FailedModulusCheckError extends BarsRequestError("FAILED_MODULUS_CHECK")
case object AdditionalInfoRequiredError extends BarsRequestError("ADDITIONAL_INFORMATION_REQUIRED")
case object DirectCreditUnsupportedError extends BarsRequestError("DIRECT_CREDIT_UNSUPPORTED")

case class ErrorsInResponseError(field: String) extends BarsFailedCheckError(field + "_ERROR")
case class IndeterminateResultError(field: String) extends BarsFailedCheckError(field + "_INDETERMINATE")
