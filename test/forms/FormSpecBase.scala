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

import base.SpecBase
import play.api.data.{Form, FormError}

trait FormSpecBase extends SpecBase {

  def handleForMandatoryField[A](form: Form[A])
                                (key: String,
                                 minLength: Int,
                                 maxLength: Int,
                                 invalidValues: Seq[String],
                                 validValues: Seq[String],
                                 regex: String): Unit = {
    s"for mandatory field - $key" - {
      "should return an error when field is missing" in {
        val result: Form[A] = form.bind(Map.empty)
        result.error(key) mustBe Some(FormError(key, s"$key.formError.required"))
      }

      if (minLength == maxLength && minLength > 1) {
        "should return an error when field has incorrect length" in {
          val result: Form[A] = form.bind(Map(key -> "a"))
          result.error(key) mustBe Some(FormError(key, s"$key.formError.length", Seq(minLength)))
        }
      } else {
        if (minLength > 1) {
          "should return an error when field is too short" in {
            val result: Form[A] = form.bind(Map(key -> "a"))
            result.error(key) mustBe Some(FormError(key, s"$key.formError.length", Seq(minLength)))
          }
        }

        "should return an error when field is too long" in {
          val result: Form[A] = form.bind(Map(key -> "a" * (maxLength + 1)))
          result.error(key) mustBe Some(FormError(key, s"$key.formError.length", Seq(maxLength)))
        }
      }

      "should not accept a field containing only whitespace" in {
        val result: Form[A] = form.bind(Map(key -> "     "))
        result.error(key) mustBe Some(FormError(key, s"$key.formError.required"))
      }

      invalidValues.foreach(invalidValue => {
        s"should return errors for invalid value: $invalidValue" in {
          val result: Form[A] = form.bind(Map(key -> invalidValue))
          result.error(key) mustBe Some(FormError(key, s"$key.formError.format", Seq(regex)))
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

  def handleForOptionalField[A](form: Form[A])
                               (key: String,
                                minLength: Int,
                                maxLength: Int,
                                invalidValues: Seq[String],
                                validValues: Seq[String],
                                regex: String): Unit = {
    s"for optional field- $key" - {
      "should return None when field is missing" in {
        val result: Form[A] = form.bind(Map.empty)
        result.error(key) mustBe None
        result.data.get(key) mustBe None
      }

      if (minLength == maxLength && minLength > 1) {
        "should return an error when field has incorrect length" in {
          val result: Form[A] = form.bind(Map(key -> "a"))
          result.error(key) mustBe Some(FormError(key, s"$key.formError.length", Seq(minLength)))
        }
      } else {
        if (minLength > 1) {
          "should return an error when field is too short" in {
            val result: Form[A] = form.bind(Map(key -> "a"))
            result.error(key) mustBe Some(FormError(key, s"$key.formError.length", Seq(minLength)))
          }
        }

        "should return an error when field is too long" in {
          val result: Form[A] = form.bind(Map(key -> "a" * (maxLength + 1)))
          result.error(key) mustBe Some(FormError(key, s"$key.formError.length", Seq(maxLength)))
        }
      }

      "should return None for a field containing only whitespace" in {
        val result: Form[A] = form.bind(Map(key -> "     "))
        result.error(key) mustBe None
      }

      invalidValues.foreach(invalidValue => {
        s"should return errors for invalid value: $invalidValue" in {
          val result: Form[A] = form.bind(Map(key -> invalidValue))
          result.error(key) mustBe Some(FormError(key, s"$key.formError.format", Seq(regex)))
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
