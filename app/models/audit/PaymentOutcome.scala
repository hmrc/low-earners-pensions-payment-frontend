package models.audit

import play.api.libs.json.{JsString, Writes}

enum PaymentOutcome {
  case pass, fail, skipped
}

object PaymentOutcome {
  implicit val writes: Writes[PaymentOutcome] = (o: PaymentOutcome) => JsString(o.toString)
}