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

package controllers.common

import base.SpecBase
import controllers.actions.fakes.{FakeDataRetrievalAction, FakeIdentifierAction}
import controllers.actions.{DataRetrievalAction, IdentifierAction}
import models.userAnswers.UserAnswers
import play.api.mvc.Results.ImATeapot
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesControllerComponents
import services.SessionCacheService

import scala.concurrent.Future

class LeppBaseControllerSpec extends SpecBase {
  private trait Test {
    private val userAnswers: UserAnswers = UserAnswers("1")
    val mockAuth: IdentifierAction = FakeIdentifierAction(nino = nino)
    val mockSessionService: SessionCacheService = mock[SessionCacheService]
    private val mockCc: MessagesControllerComponents = stubMessagesControllerComponents()
    private lazy val mockData: DataRetrievalAction = FakeDataRetrievalAction(userAnswers)
    
    class DummyController(identifierAction: IdentifierAction,
                          data: DataRetrievalAction,
                          val controllerComponents: MessagesControllerComponents = mockCc)
      extends LeppBaseController(identifierAction, data)
    
    lazy val controller: DummyController = new DummyController(mockAuth, mockData)
  }
  
  "LeppBaseController" - {
    "handle" - {
      "should invoke block when authorisation succeeds" in new Test {
        val result: Future[Result] = controller.handle(
          _ => Future.successful(ImATeapot("Teapot time"))
        )(FakeRequest())
        
        status(result) mustBe IM_A_TEAPOT
        contentAsString(result) mustBe "Teapot time"
      }

      "should not invoke block when authorisation fails" in new Test {
        override val mockAuth = FakeIdentifierAction(true, nino)
        
        val result: Future[Result] = controller.handle(
          _ => Future.successful(ImATeapot("Teapot time"))
        )(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("some-url")
      }
    }
  }
}
