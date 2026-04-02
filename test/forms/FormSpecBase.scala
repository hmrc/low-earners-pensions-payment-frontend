package forms

import base.SpecBase
import play.api.data.{Form, FormError}

trait FormSpecBase extends SpecBase {

  def handleForMandatoryField[A](form: Form[A], keyPrefix: String)
                                (key: String,
                                 minLength: Int,
                                 maxLength: Int,
                                 invalidValues: Seq[String],
                                 validValues: Seq[String],
                                 regex: String): Unit = {
    s"for mandatory field - $key" - {
      "should return an error when field is missing" in {
        val result: Form[A] = form.bind(Map.empty)
        result.error(key) mustBe Some(FormError(key, s"$keyPrefix.formError.required.$key"))
      }

      if (minLength == maxLength && minLength > 1) {
        "should return an error when field has incorrect length" in {
          val result: Form[A] = form.bind(Map(key -> "a"))
          result.error(key) mustBe Some(FormError(key, s"$keyPrefix.formError.length.$key", Seq(minLength)))
        }
      } else {
        if (minLength > 1) {
          "should return an error when field is too short" in {
            val result: Form[A] = form.bind(Map(key -> "a"))
            result.error(key) mustBe Some(FormError(key, s"$keyPrefix.formError.length.$key", Seq(minLength)))
          }
        }

        "should return an error when field is too long" in {
          val result: Form[A] = form.bind(Map(key -> "a" * (maxLength + 1)))
          result.error(key) mustBe Some(FormError(key, s"$keyPrefix.formError.length.$key", Seq(maxLength)))
        }
      }

      "should not accept a field containing only whitespace" in {
        val result: Form[A] = form.bind(Map(key -> "     "))
        result.error(key) mustBe Some(FormError(key, s"$keyPrefix.formError.required.$key"))
      }

      invalidValues.foreach(invalidValue => {
        s"should return errors for invalid value: $invalidValue" in {
          val result: Form[A] = form.bind(Map(key -> invalidValue))
          result.error(key) mustBe Some(FormError(key, s"$keyPrefix.formError.format.$key", Seq(regex)))
        }
      })

      validValues.foreach(validValue => {
        s"should return no errors for valid value: $validValue" in {
          val result: Form[A] = form.bind(Map(key -> validValue))
          result.error(key) mustBe None
        }
      })
    }
  }

  def handleForOptionalField[A](form: Form[A], keyPrefix: String)
                               (key: String,
                                minLength: Int,
                                maxLength: Int,
                                invalidValues: Seq[String],
                                validValues: Seq[String],
                                regex: String): Unit = {
    s"for optional field- $key" - {
      "should return None error when field is missing" in {
        val result: Form[A] = form.bind(Map.empty)
        result.error(key) mustBe None
        result.data.get(key) mustBe None
      }

      if (minLength == maxLength && minLength > 1) {
        "should return an error when field has incorrect length" in {
          val result: Form[A] = form.bind(Map(key -> "a"))
          result.error(key) mustBe Some(FormError(key, s"$keyPrefix.formError.length.$key", Seq(minLength)))
        }
      } else {
        if (minLength > 1) {
          "should return an error when field is too short" in {
            val result: Form[A] = form.bind(Map(key -> "a"))
            result.error(key) mustBe Some(FormError(key, s"$keyPrefix.formError.length.$key", Seq(minLength)))
          }
        }

        "should return an error when field is too long" in {
          val result: Form[A] = form.bind(Map(key -> "a" * (maxLength + 1)))
          result.error(key) mustBe Some(FormError(key, s"$keyPrefix.formError.length.$key", Seq(maxLength)))
        }
      }

      "should return None a field containing only whitespace" in {
        val result: Form[A] = form.bind(Map(key -> "     "))
        result.error(key) mustBe None
      }

      invalidValues.foreach(invalidValue => {
        s"should return errors for invalid value: $invalidValue" in {
          val result: Form[A] = form.bind(Map(key -> invalidValue))
          result.error(key) mustBe Some(FormError(key, s"$keyPrefix.formError.format.$key", Seq(regex)))
        }
      })

      validValues.foreach(validValue => {
        s"should return no errors for valid value: $validValue" in {
          val result: Form[A] = form.bind(Map(key -> validValue))
          result.error(key) mustBe None
        }
      })
    }
  }
}
