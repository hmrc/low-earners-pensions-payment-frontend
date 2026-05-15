package views.components

import base.SpecBase
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import views.html.components.link_element

class LinkElementSpec extends SpecBase {
  "link_element" - {
    "should return the expected HTML element" in new Setup {
      val element: Document = view("/", "common.signOut")
      element.html() must include("""<a class="govuk-link govuk-link--no-visited-state" href="/">""")
      element.html() must include("Sign out")
    }
  }
  trait Setup {
    val app: Application = applicationBuilder(emptyUserAnswers).build()
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")

    def view(href: String, messageKey: String): Document = Jsoup.parse(
      app.injector.instanceOf[link_element].apply(href, messageKey).body
    )
  }
}
