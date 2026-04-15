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
sealed abstract class FailedCheckError(reason: String) extends BarsError(reason)
sealed abstract class RequestError(reason: String) extends BarsError(reason)

case object SortCodeNotFoundError extends RequestError("SORT_CODE_NOT_FOUND")
case object AccountNotFoundError extends RequestError("ACCOUNT_NOT_FOUND")
case object NameMismatchError extends RequestError("SUPPLIED_NAME_NOT_MATCHED")
case object FailedModulusCheckError extends RequestError("FAILED_MODULUS_CHECK")
case object AdditionalInfoRequiredError extends RequestError("ADDITIONAL_INFORMATION_REQUIRED")
case object DirectCreditUnsupportedError extends RequestError("DIRECT_CREDIT_UNSUPPORTED")

case object BarsCheckFailedError extends FailedCheckError("ERRORS_IN_BARS_RESPONSE")
case object IndeterminateResultError extends FailedCheckError("COULD_NOT_DETERMINE_RESULT")
