package models.bars

import base.SpecBase
import models.bars.statuses.NameMatches
import play.api.libs.json.{JsError, JsString}

class NameMatchesSpec extends SpecBase {
  "BarsResponseNameMatches" - {
    "reads" - {
      Seq(
        ("yes", NameMatches.Yes),
        ("no", NameMatches.No),
        ("partial", NameMatches.Partial),
        ("indeterminate", NameMatches.Indeterminate),
        ("inapplicable", NameMatches.Inapplicable)
      ).foreach((s, m) => enumReadsTest(s, m))
      
      "should not read for an invalid value" in {
        JsString("nope").validate[NameMatches] mustBe a[JsError]
      }
    }
  }
}
