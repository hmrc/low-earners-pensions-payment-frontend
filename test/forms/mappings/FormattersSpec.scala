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
    val strFormatter: Formatter[String] = stringFormatter("error")
    val optStrFormatter: Formatter[Option[String]] = optStringFormatter()
  }

  "Formatters" - {
    "stringFormatter" - {
      "must bind a valid string" in new Test {
        val result: Either[Seq[FormError], String] = strFormatter.bind("value", Map("value" -> "foobar"))
        result.toOption mustBe Some("foobar")
      }

      "must not bind an empty string" in new Test {
        val result: Either[Seq[FormError], String] = strFormatter.bind("value", Map("value" -> ""))
        result.swap.getOrElse(Nil) must contain(FormError("value", "error"))
      }

      "must not bind a string of whitespace only" in new Test {
        val result: Either[Seq[FormError], String] = strFormatter.bind("value", Map("value" -> " \t"))
        result.swap.getOrElse(Nil) must contain(FormError("value", "error"))
      }

      "must not bind an empty map" in new Test {
        val result: Either[Seq[FormError], String] = strFormatter.bind("value", Map.empty[String, String])
        result.swap.getOrElse(Nil) must contain(FormError("value", "error"))
      }

      "must unbind a valid value" in new Test {
        val result: Map[String, String] = strFormatter.unbind("value", "foo")
        result.apply("value") mustBe "foo"
      }
    }

    "optStringFormatter" - {
      "must bind a valid string" in new Test {
        val result: Either[Seq[FormError], Option[String]] = optStrFormatter.bind("value", Map("value" -> "foobar"))
        result mustBe a[Right[_, _]]
        result.getOrElse(None) mustBe Some("foobar")
      }

      "must bind an empty string" in new Test {
        val result: Either[Seq[FormError], Option[String]] = optStrFormatter.bind("value", Map("value" -> ""))
        result mustBe a[Right[_, _]]
        result.getOrElse(Some("N/A")) mustBe None
      }

      "must bind a string of whitespace only" in new Test {
        val result: Either[Seq[FormError], Option[String]] = optStrFormatter.bind("value", Map("value" -> " \t"))
        result mustBe a[Right[_, _]]
        result.getOrElse(Some("N/A")) mustBe None
      }

      "must bind when value is not provided" in new Test {
        val result: Either[Seq[FormError], Option[String]] = optStrFormatter.bind("value", Map.empty[String, String])
        result mustBe a[Right[_, _]]
        result.getOrElse(Some("N/A")) mustBe None
      }

      "must unbind a valid non-empty value" in new Test {
        val result: Map[String, String] = optStrFormatter.unbind("value", Some("foo"))
        result.get("value") mustBe Some("foo")
      }

      "must unbind an empty value" in new Test {
        val result: Map[String, String] = optStrFormatter.unbind("value", None)
        result.get("value") mustBe None
      }
    }
  }
}
