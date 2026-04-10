package models.bars.statuses

import play.api.libs.json.*

enum NonStandardAccountDetails {
  case Yes, No, InApplicable
}

object NonStandardAccountDetails {
  implicit val reads: Reads[NonStandardAccountDetails] = Reads{
    case JsString("yes") => JsSuccess(Yes)
    case JsString("no") => JsSuccess(No)
    case JsString("inapplicable") => JsSuccess(InApplicable)
    case _ => JsError("error.nonStandardAccountDetailsRequiredForBacs.invalid")
  }
}