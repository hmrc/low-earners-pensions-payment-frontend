package models.bars.statuses

import play.api.libs.json.*

enum AccountNumberWellFormatted {
  case Yes, No, Indeterminate
}

object AccountNumberWellFormatted {
  implicit val reads: Reads[AccountNumberWellFormatted] = Reads{
    case JsString("yes") => JsSuccess(Yes)
    case JsString("no") => JsSuccess(No)
    case JsString("indeterminate") => JsSuccess(Indeterminate)
    case _ => JsError("error.accountNumberIsWellFormatted.invalid")
  }
}