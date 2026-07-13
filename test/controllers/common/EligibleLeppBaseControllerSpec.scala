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
import controllers.actions.fakes.{FakeDataRetrievalAction, FakeIdentifierAction, FakeStartPageCheckEligibilityActionBuilder}
import controllers.actions.{CheckEligibilityAction, DataRetrievalAction, IdentifierAction}
import models.userAnswers.{LeppSummary, UserAnswers}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.libs.json.{JsBoolean, JsObject, JsString, Json}
import play.api.mvc.Results.ImATeapot
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesControllerComponents

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EligibleLeppBaseControllerSpec extends SpecBase {
  trait Test {
    val userAnswers: UserAnswers = UserAnswers("1")
    val mockAuth: IdentifierAction = FakeIdentifierAction(nino = nino)
    
    lazy val mockGetData: DataRetrievalAction = FakeDataRetrievalAction(
      dataToReturn = userAnswers,
      lockoutTimestampOpt = None
    )
    
    val eligibilityResult: Right[Unit, LeppSummary] = Right(summaryModel)
    
    lazy val mockCheckEligibility: CheckEligibilityAction = FakeStartPageCheckEligibilityActionBuilder(eligibilityResult)(
      using mockSessionService, mockCidHandler, mockRetrievalService
    ).create(withCaching = true)
    
    class TestClass(identifierAction: IdentifierAction,
                    retrievalAction: DataRetrievalAction,
                    checkEligibilityAction: CheckEligibilityAction) extends EligibleLeppBaseController(
      identify = identifierAction,
      getData = retrievalAction,
      checkEligibility = checkEligibilityAction
    ) {

      override protected def controllerComponents: MessagesControllerComponents = stubMessagesControllerComponents()
    }
    
    lazy val controller: TestClass = new TestClass(mockAuth, mockGetData, mockCheckEligibility)
  }
  
  "EligibleLeppBaseController" - {
    "handleWithSubmissionCheck" - {
      "should redirect to clear cache controller when data has already submitted" in new Test {
        when(
          mockSessionService.save(userAnswers = ArgumentMatchers.any())(
            hc = ArgumentMatchers.any(),
            ec = ArgumentMatchers.any()
          )
        ).thenReturn(Future.successful(()))

        override val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = JsObject(Seq("isSubmitted" -> JsBoolean(true)))
        )

        lazy val result: Future[Result] = controller.handleWithSubmissionCheck(
          req => Future.successful(ImATeapot(req.userAnswers.data))
        )(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.ClearCacheController.onPageLoad().url)
      }

      "should not wipe user answers when data has not been submitted" in new Test {
        when(
          mockSessionService.save(
            ArgumentMatchers.any()
          )(
            ArgumentMatchers.any(),
            ArgumentMatchers.any()
          )
        ).thenReturn(
          Future.successful(())
        )

        override val userAnswers: UserAnswers = UserAnswers(
          id = "1",
          data = JsObject(Seq("otherData" -> JsString("value")))
        )

        lazy val result: Future[Result] = controller.handleWithSubmissionCheck(
          req => Future.successful(ImATeapot(req.userAnswers.data))
        )(FakeRequest())

        status(result) mustBe IM_A_TEAPOT
        contentAsJson(result) mustBe Json.parse("""{"otherData": "value"}""")
      }
    }
  }
}
