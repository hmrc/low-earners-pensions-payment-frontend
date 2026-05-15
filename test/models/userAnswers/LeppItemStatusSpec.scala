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

package models.userAnswers

import base.SpecBase
import models.userAnswers.LeppItemStatus.*

class LeppItemStatusSpec extends SpecBase {
  "LeppItemStatus" - {
    "reads" - {
      Seq(
        ("Available", Available),
        ("Paid", Paid),
        ("Suspended", Suspended),
        ("Cancelled", Cancelled),
        ("Unsupported", Unsupported)
      ).foreach(enumReadsTest[LeppItemStatus])
    }

    "writes" - {
      Seq(
        (Available, "Available"),
        (Paid, "Paid"),
        (Suspended, "Suspended"),
        (Cancelled, "Cancelled"),
        (Unsupported, "Unsupported")
      ).foreach(enumWritesTest[LeppItemStatus])
    }

    "toHtmlClass" - {
      Seq(
        (Available, "govuk-tag--blue"),
        (Paid, "govuk-tag--green"),
        (Suspended, "govuk-tag--yellow"),
        (Cancelled, "govuk-tag--red"),
        (Unsupported, "")
      ).foreach((status, expectedHtmlClass) =>
        s"for status: ${status.toString} should return a class of: $expectedHtmlClass" in {
          status.getHtmlClass mustBe expectedHtmlClass
        })
    }
  }

}
