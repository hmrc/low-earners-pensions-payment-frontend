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

package controllers

import base.SpecBase
import controllers.actions.fakes.FakeStartPageCheckEligibilityActionBuilder
import play.api.Application
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WhatYouWillNeedControllerSpec extends SpecBase {

  "What you will need controller" - {
    "must redirect to ineligible page if use has no LEPP data" in {
      val fakeStartPageCheckEligibilityActionBuilder = new FakeStartPageCheckEligibilityActionBuilder(
        Left(())
      )
      val application: Application = applicationBuilder(
        startPageCheckEligibilityBuilder = fakeStartPageCheckEligibilityActionBuilder
      ).build()

      running(application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
          method = GET,
          path = controllers.routes.WhatYouWillNeedController.onPageLoad().url
        )

        val result: Future[Result] = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.auth.routes.IneligibleController.onPageLoad().url)
      }
    }

    "must return OK and the correct view for a GET" in {
      val application: Application = applicationBuilder().build()

      running(application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
          method = GET,
          path = controllers.routes.WhatYouWillNeedController.onPageLoad().url
        )

        val result: Future[Result] = route(application, request).value
        status(result) mustEqual OK
      }
    }

    "must redirect to start page for a GET of / " in {
      val application: Application = applicationBuilder().build()

      running(application) {
        implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(
          method = GET,
          path = controllers.routes.WhatYouWillNeedController.start.url
        )

        val result: Future[Result] = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.WhatYouWillNeedController.onPageLoad().url)
      }
    }
  }
}
