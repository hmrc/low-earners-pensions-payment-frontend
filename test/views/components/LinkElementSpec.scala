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
      val element: Document = view("/", "common.signOut", "")
      element.html() must include("""<a class="govuk-link govuk-link--no-visited-state" target="" href="/">""")
      element.html() must include("Sign out")

      val element1: Document = view("/", "common.signOut", "_blank")
      element1.html() must include("""<a class="govuk-link govuk-link--no-visited-state" target="_blank" href="/">""")
    }
  }
  trait Setup {
    val app: Application = applicationBuilder(emptyUserAnswers).build()
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")

    def view(href: String, messageKey: String, target: String): Document = Jsoup.parse(
      app.injector.instanceOf[link_element].apply(href, messageKey, target).body
    )
  }
}
