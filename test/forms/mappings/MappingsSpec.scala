package forms.mappings

import base.SpecBase
import play.api.data.FormError

class MappingsSpec extends SpecBase {
  
  private trait Test extends Mappings
  
  "Mappings" - {
    "text" - {
      "should return an error when field is missing" in new Test {
        val result: Either[Seq[FormError], String] = stringFormatter("errorKey").bind("field", Map.empty)
        result mustBe a[Left[_, _]]
        result.swap.getOrElse(Nil) must contain(FormError("field", List("errorKey")))
      }

      "should return an error when field cannot bind to string" in new Test {
        val result: Either[Seq[FormError], String] = stringFormatter("errorKey").bind("field", Map("field" -> "   "))
        result mustBe a[Left[_, _]]
        result.swap.getOrElse(Nil) must contain(FormError("field", List("errorKey")))
      }

      "should bind for a valid value" in new Test {
        val result: Either[Seq[FormError], String] = stringFormatter("errorKey").bind("field", Map("field" -> "value"))
        result mustBe a[Right[_, _]]
        result.getOrElse("N/A") mustBe "value"
      }
    }
  }

}
