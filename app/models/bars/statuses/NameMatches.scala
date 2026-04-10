package models.bars.statuses

import play.api.libs.json.*

enum NameMatches {
  case Yes, No, Partial, Inapplicable, Indeterminate, Error
}

object NameMatches {
  implicit val reads: Reads[NameMatches] = Reads{
    case JsString("yes") => JsSuccess(Yes)
    case JsString("no") => JsSuccess(No)
    case JsString("partial") => JsSuccess(Partial)
    case JsString("inapplicable") => JsSuccess(Inapplicable)
    case JsString("indeterminate") => JsSuccess(Indeterminate)
    case JsString("error") => JsSuccess(Error)
    case _ => JsError("error.nonStandardAccountDetailsRequiredForBacs.invalid")
  }
}