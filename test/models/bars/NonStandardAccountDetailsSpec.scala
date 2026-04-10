package models.bars

import base.SpecBase
import models.bars.statuses.NonStandardAccountDetails
import play.api.libs.json.{JsError, JsString}

class NonStandardAccountDetailsSpec extends SpecBase {
  "BarsResponseNonStandardAccountDetails" - {
    "reads" - {
      Seq(
        ("yes", NonStandardAccountDetails.Yes),
        ("no", NonStandardAccountDetails.No),
        ("inapplicable", NonStandardAccountDetails.InApplicable)
      ).foreach((s, m) => enumReadsTest(s, m))
      
      "should not read for an invalid value" in {
        JsString("nope").validate[NonStandardAccountDetails] mustBe a[JsError]
      }
    }
  }
}
