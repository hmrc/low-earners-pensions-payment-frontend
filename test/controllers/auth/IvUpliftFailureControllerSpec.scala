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
      val result: Document = Jsoup.parse(contentAsString(controller.onPageLoad(None)(request)))
      result.toString mustBe view.toString
    }
  }

}
