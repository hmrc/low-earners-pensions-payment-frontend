package models.bars

import base.SpecBase
import models.bars.statuses.{AccountExists, NonStandardAccountDetails}
import play.api.libs.json.{JsError, JsString}

class AccountExistsSpec extends SpecBase {
  "BarsResponseAccountExists" - {
    "reads" - {
      Seq(
        ("yes", AccountExists.Yes),
        ("no", AccountExists.No),
        ("inapplicable", AccountExists.Inapplicable),
        ("indeterminate", AccountExists.Indeterminate),
        ("error", AccountExists.Error)
      ).foreach((s, m) => enumReadsTest(s, m))
      
      "should not read for an invalid value" in {
        JsString("nope").validate[NonStandardAccountDetails] mustBe a[JsError]
      }
    }
  }
}
