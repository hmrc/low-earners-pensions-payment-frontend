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
import models.*
import models.userAnswers.LeppItemStatus
import models.userAnswers.LeppItemStatus.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import views.html.components.dashboard.status_element

class StatusElementSpec extends SpecBase {

  "status_element" - {
    def testForStatus(status: LeppItemStatus, expectedColour: String): Unit =
      s"display correct element with colour: $expectedColour for status: ${status.toString}" in new Setup {
        val viewForStatus: Document = view(status)
        viewForStatus.html() must include("""<strong class="govuk-tag govuk-tag--""")
        viewForStatus.getElementsByClass("govuk-tag").hasClass(s"govuk-tag--$expectedColour") mustBe true
      }
      
    Seq(
      (Available, "blue"),
      (Cancelled, "red"),
      (Suspended, "yellow"),
      (Paid, "green"),
    ).foreach(testForStatus)
  }

  trait Setup {
    val app: Application = applicationBuilder(emptyUserAnswers).build()
    implicit val msg: Messages = messages(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")

    def view(status: LeppItemStatus): Document = Jsoup.parse(
      app.injector.instanceOf[status_element].apply(status).body
    )
  }
}
