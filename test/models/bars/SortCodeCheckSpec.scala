package models.bars

import base.SpecBase
import models.bars.statuses.SortCodeCheck
import play.api.libs.json.{JsError, JsString}

class SortCodeCheckSpec extends SpecBase {
  "BarsResponseSortCodeCheckField" - {
    "reads" - {
      Seq(
        ("yes", SortCodeCheck.Yes),
        ("no", SortCodeCheck.No),
        ("error", SortCodeCheck.Error)
      ).foreach((s, m) => enumReadsTest(s, m))
      
      "should not read for an invalid value" in {
        JsString("nope").validate[SortCodeCheck] mustBe a[JsError]
      }
    }
  }
}
