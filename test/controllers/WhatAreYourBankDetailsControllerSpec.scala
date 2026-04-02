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
import cats.data.EitherT
import connectors.ConnectorResponse
import controllers.actions.{DataRetrievalAction, FakeDataRetrievalAction, FakeIdentifierAction, IdentifierAction}
import controllers.validators.BarsRequestValidator
import models.CorrelationId
import models.ResponseWrapper.ErrorWrapper
import models.bars.{BarsResponse, ValidatedBarsRequest}
import models.errors.ErrorResult.ServiceErrorResult
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api.mvc.{AnyContent, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{POST, stubMessagesControllerComponents}
import services.BarsService
import utils.CorrelationIdOptional

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WhatAreYourBankDetailsControllerSpec extends SpecBase {

  trait Test {
    val mockIdentifierAction: IdentifierAction = new FakeIdentifierAction()
    val mockDataRetrievalAction: DataRetrievalAction = new FakeDataRetrievalAction(emptyUserAnswers)
      
    val mockValidator: BarsRequestValidator = mock[BarsRequestValidator]
    val mockService: BarsService = mock[BarsService]
    val mockCc: MessagesControllerComponents = stubMessagesControllerComponents()

    lazy val idHandler: CorrelationIdOptional = CorrelationIdOptional()

    lazy val testController: WhatAreYourBankDetailsController = new WhatAreYourBankDetailsController(
      identify = mockIdentifierAction,
      getData = mockDataRetrievalAction,
      validator = mockValidator,
      service = mockService,
      ???,
      ???,
      correlationIdHandler = idHandler,
      controllerComponents = mockCc
    )

    val request: FakeRequest[AnyContent] = FakeRequest(
      method = POST,
      path = "some-path"
    ).withHeaders("correlationId" -> "some-id")

    lazy val result: Future[Result] = testController.checkBankAccountDetails(
      name = Some("name"),
      accountNumber = Some("12345678"),
      sortCode = Some("11-22-33"),
      rollNumber = Some("rollNumber")
    )(request)
    
    def validatorSuccess(correlationId: CorrelationId = testCorrelationId): OngoingStubbing[Either[ErrorWrapper, ValidatedBarsRequest]] = when(
      mockValidator.validate(
        request = ArgumentMatchers.any(),
        correlationId = ArgumentMatchers.eq(correlationId)
      )
    ).thenReturn(
      Right(testValidatedBarsRequest)
    )
    
    def serviceSuccess(correlationId: CorrelationId = testCorrelationId): OngoingStubbing[ConnectorResponse[BarsResponse]] = when(
      mockService.checkBankAccountDetails(
        ArgumentMatchers.eq(testValidatedBarsRequest),
        ArgumentMatchers.eq(correlationId)
      )(
        ArgumentMatchers.any(),
        ArgumentMatchers.any()
      )
    ).thenReturn(
      EitherT(Future.successful(Right(testSuccessResponse.copy(correlationId = correlationId))))
    )
  }

  "BarsController" - {
    "for error scenarios" - {
      "should return error result when request validation fails" in new Test {
        when(
          mockValidator.validate(
            request = ArgumentMatchers.any(),
            correlationId = ArgumentMatchers.eq(CorrelationId("some-id"))
          )
        ).thenReturn(
          Left(ErrorWrapper(
            value = ServiceErrorResult(IM_A_TEAPOT, "TEST_ERROR"),
            correlationId = testCorrelationId
          ))
        )
        
        status(result) mustBe IM_A_TEAPOT
        contentAsJson(result).toString must include("TEST_ERROR")
      }

      "should return error result when BarsService returns an error response" in new Test {
        validatorSuccess()
        
        when(
          mockService.checkBankAccountDetails(
            ArgumentMatchers.eq(testValidatedBarsRequest),
            ArgumentMatchers.eq(testCorrelationId)
          )(
            ArgumentMatchers.any(),
            ArgumentMatchers.any()
          )
        ).thenReturn(
          EitherT(Future.successful(Left(testDownstreamErrorWrapper)))
        )

        status(result) mustBe IM_A_TEAPOT
        contentAsJson(result).toString must include("TEST_ERROR")
      }
    }

    "for success scenarios" - {
      "should return the expected result" in new Test {
        validatorSuccess()
        serviceSuccess()
        
        status(result) mustBe OK
        contentAsJson(result).toString must include("banky bank")
        headers(result).get("correlationId") mustBe Some("some-id")
      }

      "should generate a correlation ID if one doesn't exist" in new Test {
        override lazy val idHandler: CorrelationIdOptional = new CorrelationIdOptional {
          override def generateCorrelationId: CorrelationId = CorrelationId("generatedId")
        }

        override val request: FakeRequest[AnyContent] = FakeRequest(
          method = POST,
          path = "some-uri"
        )

        validatorSuccess(CorrelationId("generatedId"))
        serviceSuccess(CorrelationId("generatedId"))

        status(result) mustBe OK
        contentAsJson(result).toString must include("banky bank")
        headers(result).get("correlationId") mustBe Some("generatedId")
      }
    }
  }
}
