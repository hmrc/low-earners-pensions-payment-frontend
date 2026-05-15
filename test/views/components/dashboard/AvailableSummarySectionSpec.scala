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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import views.html.components.dashboard.available_summary_section

class AvailableSummarySectionSpec extends SpecBase {

  "available_summary_section" - {
    "should produce expected HTML element" in new Setup {
      val html: String = view(100.111, "/href").html
      html must include("""<strong class="govuk-!-font-weight-bold">£100.11</strong>""")
      html must include("To accept these payments, you need to provide us with your bank details.")
      html must include("""<a href="/href"""")
    }
  }

  trait Setup {
    val app: Application = applicationBuilder(emptyUserAnswers).build()
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")

    def view(entitlement: BigDecimal, continueUrl: String): Document = Jsoup.parse(
      app.injector.instanceOf[available_summary_section].apply(entitlement, continueUrl).body
    )
  }
}
