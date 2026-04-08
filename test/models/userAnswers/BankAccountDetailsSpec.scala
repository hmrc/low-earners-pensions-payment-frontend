package models.userAnswers

import base.SpecBase
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json, JsObject}

class BankAccountDetailsSpec extends SpecBase {
  "BankAccountDetails" - {
    val json: JsValue = Json.parse(
      """
        |{
        | "name": "name",
        | "accountNumber": "number",
        | "sortCode": "sortcode",
        | "rollNumber": "rollNumber"
        |}
      """.stripMargin
    )

    val model: BankAccountDetails = BankAccountDetails("name", "number", "sortcode", Some("rollNumber"))

    "reads" - {
      "should return a JsSuccess for valid JSON" in {
        json.validate[BankAccountDetails] mustBe a[JsSuccess[_]]
        json.as[BankAccountDetails] mustBe model
      }

      "should return a JsError when mandatory fields are missing" in {
        Json.parse(
          """
            |{
            | "name": true,
            | "accountNumber": 1.1,
            | "sortCode": [],
            | "rollNumber": {}
            |}
          """.stripMargin
        ).validate[BankAccountDetails] mustBe a[JsError]
      }

      "should return a JsError when fields have incorrect data types" in {
        JsObject.empty.validate[BankAccountDetails] mustBe a[JsError]
      }
    }

    "writes" - {
      "should produce the expected JSON" in {
        Json.toJson(model) mustBe json
      }
    }
  }
}
