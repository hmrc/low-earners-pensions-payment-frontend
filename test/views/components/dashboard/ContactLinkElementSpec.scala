package views.components.dashboard

import base.SpecBase
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import views.html.components.dashboard.contact_link_element

class ContactLinkElementSpec extends SpecBase {
  "link_element" - {
    "should return the expected HTML element" in new Setup {
      val element: Document = view()
      element.html() must include("For more information,")
      element.html() must include("""<a class="govuk-link govuk-link--no-visited-state" href="/">contact us (opens in new tab)</a>""")
    }
  }
  trait Setup {
    val app: Application = applicationBuilder(emptyUserAnswers).build()
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")

    def view(): Document = Jsoup.parse(
      app.injector.instanceOf[contact_link_element].apply().body
    )
  }
}
