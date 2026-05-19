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
import connectors.barsLockout.BarsVerifyStatusConnector
import controllers.actions.*
import models.userAnswers.UserAnswers
import play.api.mvc.Results.ImATeapot
import play.api.mvc.{DefaultActionBuilder, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesControllerComponents
import services.SessionCacheService

import scala.concurrent.{ExecutionContext, Future}

class BarsLeppBaseControllerSpec extends SpecBase {
  private trait Test(count: Int) {
    private val userAnswers: UserAnswers = UserAnswers("1")
    val mockAuth: IdentifierAction = FakeIdentifierAction()
    private val mockBarsConnector: BarsVerifyStatusConnector = mock[BarsVerifyStatusConnector]
    private val mockBarsLockoutAction: BarsLockoutAction = FakeBarsLockoutAction(mockBarsConnector, count)
    private val mockSessionService: SessionCacheService = mock[SessionCacheService]
    private val mockCc: MessagesControllerComponents = stubMessagesControllerComponents()
    private lazy val mockData: DataRetrievalAction = FakeDataRetrievalAction(userAnswers)

    val defaultActionBuilder: DefaultActionBuilder = app.injector.instanceOf[DefaultActionBuilder]
    
    class DummyController(identifierAction: IdentifierAction,
                          data: DataRetrievalAction,
                          barsLockoutAction: BarsLockoutAction,
                          val controllerComponents: MessagesControllerComponents = mockCc)
      extends BarsLeppBaseController(identifierAction, data, barsLockoutAction) with SessionDataHandling {
      
      override val sessionService: SessionCacheService = mockSessionService
      override implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    }
    
    lazy val controller: DummyController = new DummyController(mockAuth, mockData, mockBarsLockoutAction)
  }
  
  "BarsLeppBaseController" - {
    "handle" - {
      "should invoke bars refiner when authorisation succeeds" in new Test(1) {
        val result: Future[Result] = controller.handle(
          _ => Future.successful(ImATeapot("Teapot time"))
        )(FakeRequest())
        
        status(result) mustBe IM_A_TEAPOT
        contentAsString(result) mustBe "Teapot time"
      }

      "should not bars refiner when authorisation fails" in new Test(1) {
        override val mockAuth = FakeIdentifierAction(true)
        
        val result: Future[Result] = controller.handle(
          _ => Future.successful(ImATeapot("Teapot time"))
        )(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("some-url")
      }

      "should redirect to BarsLockout when authorisation succeed but exceeds BARS limit" in new Test(3) {
        
        val result: Future[Result] = controller.handle(
          _ => Future.successful(ImATeapot("Teapot time"))
        )(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(bars.routes.BarsLockoutController.barsLockout.url)
      }
    }
  }
}
