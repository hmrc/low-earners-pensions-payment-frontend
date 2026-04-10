package models.bars

import base.SpecBase
import models.bars.statuses.AccountNumberWellFormatted
import play.api.libs.json.{JsError, JsString}

class AccountNumberWellFormattedSpec extends SpecBase {
  "BarsResponseAccountNumberWellFormatted" - {
    "reads" - {
      Seq(
        ("yes", AccountNumberWellFormatted.Yes),
        ("no", AccountNumberWellFormatted.No),
        ("indeterminate", AccountNumberWellFormatted.Indeterminate)
      ).foreach((s, m) => enumReadsTest(s, m))
      
      "should not read for an invalid value" in {
        JsString("nope").validate[AccountNumberWellFormatted] mustBe a[JsError]
      }
    }
  }
}
