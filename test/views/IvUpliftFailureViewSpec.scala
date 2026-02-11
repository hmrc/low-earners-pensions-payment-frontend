package views

import base.SpecBase
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import views.html.IvUpliftFailureView

class IvUpliftFailureViewSpec extends SpecBase {

  "view" - {
    "display correct error information" in new Setup {
      view.getElementsByTag("h1").text() mustBe messages(app)("ivUpliftFailure.title")
      view.getElementsByClass("govuk-body").text().contains(messages(app)("ivUpliftFailure.p1"))
      view.getElementsByClass("govuk-body").text().contains(messages(app)("ivUpliftFailure.li1"))
      view.getElementsByClass("govuk-body").text().contains(messages(app)("ivUpliftFailure.li2"))
      view.getElementsByClass("govuk-body").text().contains(messages(app)("ivUpliftFailure.li3"))
    }
  }
  
  trait Setup {
    val app: Application = applicationBuilder(emptyUserAnswers).build()
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")

    val view: Document = Jsoup.parse(app.injector.instanceOf[IvUpliftFailureView].apply().body)
  }
}
