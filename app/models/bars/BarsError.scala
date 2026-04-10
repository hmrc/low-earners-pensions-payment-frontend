package models.bars

sealed abstract class BarsError(reason: String)

case object BarsCheckFailedError extends BarsError("ERRORS_IN_BARS_RESPONSE")
case object SortCodeNotFoundError extends BarsError("SORT_CODE_NOT_FOUND")

case object DirectCreditUnsupportedError extends BarsError("DIRECT_CREDIT_UNSUPPORTED")
case object AdditionalInfoRequiredError extends BarsError("ADDITIONAL_INFORMATION_REQUIRED")
case object NameMismatchError extends BarsError("SUPPLIED_NAME_NOT_MATCHED")
case object AccountNotFoundError extends BarsError("ACCOUNT_NOT_FOUND")
case object FailedModulusCheckError extends BarsError("FAILED_MODULUS_CHECK")
