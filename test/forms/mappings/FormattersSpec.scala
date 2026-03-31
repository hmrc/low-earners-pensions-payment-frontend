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
import play.api.data.FormError
import play.api.data.format.Formatter

class FormattersSpec extends SpecBase {
  
  private trait Test extends Formatters {
    val testStringFormatter: Formatter[String] = stringFormatter("error")
  }

  "Formatters" - {
    "stringFormatter" - {
      "must bind a valid string" in new Test {
        val result: Either[Seq[FormError], String] = testStringFormatter.bind("value", Map("value" -> "foobar"))
        result.toOption mustBe Some("foobar")
      }

      "must not bind an empty string" in new Test {
        val result: Either[Seq[FormError], String] = testStringFormatter.bind("value", Map("value" -> ""))
        result.swap.getOrElse(Nil) must contain(FormError("value", "error"))
      }

      "must not bind a string of whitespace only" in new Test {
        val result: Either[Seq[FormError], String] = testStringFormatter.bind("value", Map("value" -> " \t"))
        result.swap.getOrElse(Nil) must contain(FormError("value", "error"))
      }

      "must not bind an empty map" in new Test {
        val result: Either[Seq[FormError], String] = testStringFormatter.bind("value", Map.empty[String, String])
        result.swap.getOrElse(Nil) must contain(FormError("value", "error"))
      }

      "must unbind a valid value" in new Test {
        val result: Map[String, String] = testStringFormatter.unbind("value", "foo")
        result.apply("value") mustBe "foo"
      }
    }
  }
}
