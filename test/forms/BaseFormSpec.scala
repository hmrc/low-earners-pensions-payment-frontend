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

import play.api.data.Form
import play.api.data.Forms.mapping

class BaseFormSpec extends FormSpecBase {

  private case class FormData(data: String,
                              dataWithFixedLength: String,
                              dataOpt: Option[String],
                              dataOptWithFixedLength: Option[String])

  private object FormData {
    def unapply(formData: FormData): Option[(String, String, Option[String], Option[String])] = Some(
      formData.data,
      formData.dataWithFixedLength,
      formData.dataOpt,
      formData.dataOptWithFixedLength
    )
  }

  private class TestFormProvider extends BaseForm {
    def apply(): Form[FormData] = Form[FormData](
      mapping(
        mandatoryTextField("data", 3, 6, "[A-Z]{3,6}"),
        mandatoryTextField("dataWithFixedLength", 3, 3, "[A-Z]{3}"),
        optionalTextField("dataOpt", 3, 6, "[A-Z]{3,6}"),
        optionalTextField("dataOptWithFixedLength", 3, 3, "[A-Z]{3}")
      )(FormData.apply)(FormData.unapply)
    )
  }

  "BaseForm" - {
    "stripWhitespace" - {
      "should remove any trailing, and leading whitespace and reduce duplicated whitespace" in new TestFormProvider {
        stripWhitespace("       word     another-word     ") mustBe "word another-word"
      }
    }

    "stripOptionalWhitespace" - {
      "should remove any trailing, and leading whitespace and reduce duplicated whitespace" in new TestFormProvider {
        stripOptionalWhitespace(Some("       word     another-word     ")) mustBe Some("word another-word")
      }

      "should do nothing for an None value" in new TestFormProvider {
        stripOptionalWhitespace(None) mustBe None
      }
    }

    "mandatoryTextField" - {
      val form: Form[FormData] = (new TestFormProvider())()
      handleForMandatoryField(form)("data", 3, 6, Seq("1234"), Seq("ABCD"), "[A-Z]{3,6}")
      handleForMandatoryField(form)("dataWithFixedLength", 3, 3, Seq("123"), Seq("ABC"), "[A-Z]{3}")
    }

    "optionalTextField" - {
      val form: Form[FormData] = (new TestFormProvider())()
      handleForOptionalField(form)("dataOpt", 3, 6, Seq("1234"), Seq("ABCD"), "[A-Z]{3,6}")
      handleForOptionalField(form)("dataOptWithFixedLength", 3, 3, Seq("123"), Seq("ABC"), "[A-Z]{3}")
    }
  }
}
