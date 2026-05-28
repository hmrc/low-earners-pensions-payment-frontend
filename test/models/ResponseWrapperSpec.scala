package models

import base.SpecBase
import models.ResponseWrapper.SuccessWrapper

class ResponseWrapperSpec extends SpecBase {
  "SuccessWrapper" - {
    "map" - {
      "should correctly apply a function to the value of a SuccessWrapper" in {
        val sw: ResponseWrapper.SuccessWrapper[String] = SuccessWrapper[String](
          value = "some-string",
          correlationId = CorrelationId("cid")
        )
        
        sw.map(_.toSeq.map(_.toUpper)) mustBe SuccessWrapper(
          Vector('S', 'O', 'M', 'E', '-', 'S', 'T', 'R', 'I', 'N', 'G'),
          CorrelationId("cid")
        )
      }
    }
  }

}
