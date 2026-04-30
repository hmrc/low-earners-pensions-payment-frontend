package models.userAnswers

import play.api.libs.json.*

enum LeppItemStatus {
  case Available, Paid, Suspended, Cancelled
}

object LeppItemStatus {
  implicit val reads: Reads[LeppItemStatus] = Reads{
    case JsString("PENDING") => JsSuccess(Available)
    case JsString("PAID") => JsSuccess(Paid)
    case JsString("SUSPENDED - RLS") => JsSuccess(Suspended)
    case JsString("CANCELLED") => JsSuccess(Cancelled)
    case _ => JsError("error.claimStatus.unsupported")
  }
  
  implicit val writes: Writes[LeppItemStatus] = (o: LeppItemStatus) => JsString(o.toString)
}