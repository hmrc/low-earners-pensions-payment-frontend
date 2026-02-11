package controllers.auth

import base.SpecBase
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers.contentAsString
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import views.html.IvUpliftFailureView

class IvUpliftFailureControllerSpec extends SpecBase with DefaultAwaitTimeout {
  private trait Test {
    val app: Application = applicationBuilder(emptyUserAnswers).build()
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")

    val controller: IvUpliftFailureController = app.injector.instanceOf[IvUpliftFailureController]
    val view: Document = Jsoup.parse(app.injector.instanceOf[IvUpliftFailureView].apply().body)
  }

  "onPageLoad" - {
    "should serve correct view when request is received" in new Test {
      val result: Document = Jsoup.parse(contentAsString(controller.onPageLoad(None)(FakeRequest())))
      result.toString mustBe view.toString
    }
  }

}
