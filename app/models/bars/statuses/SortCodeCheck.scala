package models.bars.statuses

import play.api.libs.json.*

enum SortCodeCheck {
  case Yes, No, Error
}

object SortCodeCheck {
  implicit val reads: Reads[SortCodeCheck] = Reads{
    case JsString("yes") => JsSuccess(Yes)
    case JsString("no") => JsSuccess(No)
    case JsString("error") => JsSuccess(Error)
    case _ => JsError("error.sortCodeCheck.invalid")
  }
}