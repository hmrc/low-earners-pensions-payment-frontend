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

package forms.mappings

import base.SpecBase
import org.scalatest.freespec.AnyFreeSpec
import play.api.data.validation.{Invalid, Valid, ValidationResult}

class ConstraintsSpec extends SpecBase {
  
  private trait Test extends Constraints
  
  "Constraints" - {
    "firstErrorOpt" - {
      "must return Valid when all constraints pass" in new Test {
        val result: ValidationResult = firstErrorOpt(
          maxLength(10, "error.length"),
          regexp("""^\w+$""", "error.regexp")
        )(Some("foo"))
        result mustEqual Valid
      }

      "must return Invalid when the first constraint fails" in new Test {
        val result: ValidationResult = firstErrorOpt(
          maxLength(10, "error.length"),
          regexp("""^\w+$""", "error.regexp")
        )(Some("a" * 11))
        result mustEqual Invalid("error.length", 10)
      }

      "must return Invalid when the second constraint fails" in new Test {
        val result: ValidationResult = firstErrorOpt(
          maxLength(10, "error.length"),
          regexp("""^\w+$""", "error.regexp")
        )(Some(""))
        result mustEqual Invalid("error.regexp", """^\w+$""")
      }

      "must return Invalid for the first error when both constraints fail" in new Test {
        val result: ValidationResult = firstErrorOpt(
          maxLength(-1, "error.length"),
          regexp("""^\w+$""", "error.regexp")
        )(Some(""))
        result mustEqual Invalid("error.length", -1)
      }

      "must return Valid for an empty value" in new Test {
        val result: ValidationResult = firstErrorOpt(
          maxLength(-1, "error.length"),
          regexp("""^\w+$""", "error.regexp")
        )(None)
        result mustEqual Valid
      }
    }
    
    "firstError" - {
      "must return Valid when all constraints pass" in new Test {
        val result: ValidationResult = firstError(
          maxLength(10, "error.length"),
          regexp("""^\w+$""", "error.regexp")
        )("foo")
        result mustEqual Valid
      }

      "must return Invalid when the first constraint fails" in new Test {
        val result: ValidationResult = firstError(
          maxLength(10, "error.length"),
          regexp("""^\w+$""", "error.regexp")
        )("a" * 11)
        result mustEqual Invalid("error.length", 10)
      }

      "must return Invalid when the second constraint fails" in new Test {
        val result: ValidationResult = firstError(
          maxLength(10, "error.length"),
          regexp("""^\w+$""", "error.regexp")
        )("")
        result mustEqual Invalid("error.regexp", """^\w+$""")
      }

      "must return Invalid for the first error when both constraints fail" in new Test {
        val result: ValidationResult = firstError(
          maxLength(-1, "error.length"),
          regexp("""^\w+$""", "error.regexp")
        )("")
        result mustEqual Invalid("error.length", -1)
      }
    }

    "regexp" - {
      "must return Valid for an input that matches the expression" in new Test {
        val result: ValidationResult = regexp("""^\w+$""", "error.invalid")("foo")
        result mustEqual Valid
      }

      "must return Invalid for an input that does not match the expression" in new Test {
        val result: ValidationResult = regexp("""^\d+$""", "error.invalid")("foo")
        result mustEqual Invalid("error.invalid", """^\d+$""")
      }
    }

    "minLength" - {
      "must return Valid for a string longer than the minimum length" in new Test {
        val result: ValidationResult = minLength(8, "error.length")("a" * 9)
        result mustEqual Valid
      }

      "must return Invalid for an empty string" in new Test {
        val result: ValidationResult = minLength(10, "error.length")("")
        result mustEqual Invalid("error.length", 10)
      }

      "must return Valid for a string equal to the minimum length" in new Test {
        val result: ValidationResult = minLength(10, "error.length")("a" * 10)
        result mustEqual Valid
      }

      "must return Invalid for a string shorter than the minimum length" in new Test {
        val result: ValidationResult = minLength(11, "error.length")("a" * 10)
        result mustEqual Invalid("error.length", 11)
      }
    }

    "maxLength" - {
      "must return Valid for a string shorter than the allowed length" in new Test {
        val result: ValidationResult = maxLength(10, "error.length")("a" * 9)
        result mustEqual Valid
      }

      "must return Valid for an empty string" in new Test {
        val result: ValidationResult = maxLength(10, "error.length")("")
        result mustEqual Valid
      }

      "must return Valid for a string equal to the allowed length" in new Test {
        val result: ValidationResult = maxLength(10, "error.length")("a" * 10)
        result mustEqual Valid
      }

      "must return Invalid for a string longer than the allowed length" in new Test {
        val result: ValidationResult = maxLength(10, "error.length")("a" * 11)
        result mustEqual Invalid("error.length", 10)
      }
    } 
  }
}
