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

package views.components.dashboard

import base.SpecBase
import config.AppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import views.html.components.contact_link_element

class ContactLinkElementSpec extends SpecBase {
  "link_element" - {
    "should return the expected HTML element" in new Setup {
      val element: Document = view()
      element.html() must include("For more information,")
      val expectedContactLink: String = app.injector.instanceOf[AppConfig].contactUrl
      element.html() must include(s"""<a class="govuk-link govuk-link--no-visited-state" href="$expectedContactLink">contact us (opens in new tab)</a>""")
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
