package models.userAnswers

import base.SpecBase
import LeppItemStatus.*
import play.api.libs.json.{JsError, JsString}

class LeppItemStatusSpec extends SpecBase {
  "LeppItemStatus" - {
    "reads" - {
      Seq(
        ("PENDING", Available),
        ("PAID", Paid),
        ("SUSPENDED - RLS", Suspended),
        ("CANCELLED", Cancelled)
      ).foreach(enumReadsTest[LeppItemStatus])
      
      "should return a JsError for an unsupported value" in {
        JsString("DECEASED").validate[LeppItemStatus] mustBe a[JsError]
      }
    }
    
    "writes" - {
      Seq(
        (Available, "Available"),
        (Paid, "Paid"),
        (Suspended, "Suspended"),
        (Cancelled, "Cancelled")
      ).foreach(enumWritesTest[LeppItemStatus])
    }
  }

}
