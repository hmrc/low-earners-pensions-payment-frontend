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
        ("accountName", 1, 18, Seq("!!!!!"), Seq("Mr Taxwell Payer", "Aa03'&,/\\ -"), "^[0-9A-Za-z'&,\\\\=()\\/ -]+$"),
        ("accountNumber", 6, 8, Seq("abcdefgj"), Seq("123456", "1234567", "12345678"), "^[0-9]{6,8}$"),
        ("sortCode", 6, 6, Seq("ABCDEF"), Seq("11-22-33", "112233"), "^[0-9]{6}$")
      ).foreach(handleForMandatoryField(form, "bankDetails"))

      handleForOptionalField(form, "bankDetails")(
        "rollNumber", 1, 18, Seq("!!!"), Seq("ABCDEF"), "^[A-Z0-9]{1,18}$"
      )

      "should strip any leading, trailing, or excess whitespace from fields" in {
        form.bind(Map(
          "accountName" -> " name    nameson   ",
          "accountNumber" -> "  12345678  ",
          "sortCode" -> "  112233  ",
          "rollNumber" -> "      "
        )).get mustBe BarsRequestWithMandatory("name nameson", "12345678", "112233", None)
      }

      "should strip dashes from sort code field" in {
        form.bind(Map(
          "accountName" -> "name    nameson",
          "accountNumber" -> "12345678",
          "sortCode" -> "11-22-33",
          "rollNumber" -> "ABCDEF"
        )).get mustBe BarsRequestWithMandatory("name nameson", "12345678", "112233", Some("ABCDEF"))
      }
    }
  }
}
