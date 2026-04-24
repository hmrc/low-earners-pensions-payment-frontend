/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models.bars

import base.SpecBase
import models.bars.statuses.AccountNumberWellFormatted
import play.api.libs.json.{JsError, JsString}

class AccountNumberWellFormattedSpec extends SpecBase {
  "AccountNumberWellFormatted" - {
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
