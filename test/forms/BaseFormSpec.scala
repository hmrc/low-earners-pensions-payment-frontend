package forms

import play.api.data.Form
import play.api.data.Forms.mapping

class BaseFormSpec extends FormSpecBase {

  private case class FormData(data: String, dataOpt: Option[String])

  private object FormData {
    def unapply(formData: FormData): Option[(String, Option[String])] = Some(formData.data, formData.dataOpt)
  }

  private class TestFormProvider extends BaseForm {
    def apply(): Form[FormData] = Form[FormData](
      mapping(
        mandatoryTextField("data", "prefix", 3, 6, "[A-Z]{3,6}"),
        optionalTextField("dataOpt", "prefix", 3, 6, "[A-Z]{3,6}")
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
      handleForMandatoryField(form, "prefix")("data", 3, 6, Seq("1234"), Seq("ABCD"), "[A-Z]{3,6}")
    }

    "optionalTextField" - {
      val form: Form[FormData] = (new TestFormProvider())()
      handleForOptionalField(form, "prefix")("dataOpt", 3, 6, Seq("1234"), Seq("ABCD"), "[A-Z]{3,6}")
    }
  }
}
