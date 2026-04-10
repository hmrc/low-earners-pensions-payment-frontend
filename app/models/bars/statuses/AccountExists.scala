package models.bars.statuses

import play.api.libs.json.*

enum AccountExists {
  case Yes, No, Inapplicable, Indeterminate, Error
}

object AccountExists {
  implicit val reads: Reads[AccountExists] = Reads{
    case JsString("yes") => JsSuccess(Yes)
    case JsString("no") => JsSuccess(No)
    case JsString("inapplicable") => JsSuccess(Inapplicable)
    case JsString("indeterminate") => JsSuccess(Indeterminate)
    case JsString("error") => JsSuccess(Error)
    case _ => JsError("error.nonStandardAccountDetailsRequiredForBacs.invalid")
  }
}