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

package views.bars

import base.SpecBase
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.{DefaultLangs, Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.play.language.LanguageUtils
import utils.Formatters
import views.html.bars.BarsLockoutView

import java.time.Instant

class BarsLockoutViewSpec extends SpecBase {
  
  "view" - {

    "with correct LEPP gov banner" in new Setup {
      view.getElementsByClass("govuk-service-navigation__service-name").text() mustBe messages(app)("service.name")
      view.getElementsByClass("govuk-link hmrc-sign-out-nav__link").attr("href") mustBe
        "/low-earners-pensions-payment/account/sign-out-survey"
    }

    "display correct guidance and text" in new Setup {
      view.html.contains(messages(app)("bars.lockout.p2", s"$time"))
      view.getElementById("return-to").text() mustBe
        messages(app)("bars.lockout.go-to-dashboard")
    }
  }

  trait Setup() {

    val app: Application = applicationBuilder(emptyUserAnswers).build()
    implicit val msg: Messages = messages(app)
    implicit val msgApi: MessagesApi = messageApi(app)
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("GET", "/some/resource/path")
    implicit val languageUtils: LanguageUtils = new LanguageUtils(new DefaultLangs(), app.configuration)
    
    val instant: Instant = Instant.now()
    val time: String = Formatters.fullDateTime(instant, msg, languageUtils)
    
    val view: Document = Jsoup.parse(
      app.injector.instanceOf[BarsLockoutView].apply(instant, "some-url").body
    )
  }
}
