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

package forms

import models.bars.BarsRequestWithMandatory
import play.api.data.Form

class WhatAreYourBankDetailsFormProviderSpec extends FormSpecBase {
  "WhatAreYourBankDetailsFormProvider" - {
    val formProvider: WhatAreYourBankDetailsFormProvider = WhatAreYourBankDetailsFormProvider()
    val form: Form[BarsRequestWithMandatory] = formProvider()

    "stripSortCode" - {
      "should remove any trailing whitespace, leading whitespace, and single dashes from sort-code" in {
        formProvider.stripSortCode("     11-22-33    ") mustBe "112233"
      }

      "should not remove double dashes" in {
        formProvider.stripSortCode("     11--22--33    ") mustBe "11--22--33"
      }
    }

    "formatSortCode" - {
      "should format correctly" in {
        formProvider.formatSortCode("112233") mustBe "11-22-33"
      }
    }

    "formatAccountNumber" - {
      "should format correctly" in {
        formProvider.formatAccountName("taxwell paYer") mustBe "Taxwell Payer"
      }
    }

    "bind" - {
      Seq(
        ("bankDetails.accountName", 1, 18, Seq("!!!!!"), Seq("Mr Taxwell Payer", "Aa03'&,/\\ -"), "^[0-9A-Za-z'&,\\\\=()\\/ -]+$"),
        ("bankDetails.accountNumber", 6, 8, Seq("abcdefgj"), Seq("123456", "1234567", "12345678"), "^[0-9]{6,8}$"),
        ("bankDetails.sortCode", 6, 6, Seq("ABCDEF"), Seq("11-22-33", "112233"), "^[0-9]{6}$")
      ).foreach(handleForMandatoryField(form))

      handleForOptionalField(form)(
        "bankDetails.rollNumber", 1, 18, Seq("!!!"), Seq("ABCDEF"), "^[A-Z0-9]{1,18}$"
      )

      "should strip any leading, trailing, or excess whitespace from fields" in {
        form.bind(Map(
          "bankDetails.accountName" -> " name    nameson   ",
          "bankDetails.accountNumber" -> "  12345678  ",
          "bankDetails.sortCode" -> "  112233  ",
          "bankDetails.rollNumber" -> "      "
        )).get mustBe BarsRequestWithMandatory("name nameson", "12345678", "112233", None)
      }

      "should strip dashes from sort code field" in {
        form.bind(Map(
          "bankDetails.accountName" -> "name    nameson",
          "bankDetails.accountNumber" -> "12345678",
          "bankDetails.sortCode" -> "11-22-33",
          "bankDetails.rollNumber" -> "ABCDEF"
        )).get mustBe BarsRequestWithMandatory("name nameson", "12345678", "112233", Some("ABCDEF"))
      }
    }
  }
}
