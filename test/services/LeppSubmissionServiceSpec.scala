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

package services

import base.SpecBase
import cats.data.EitherT
import connectors.{ConnectorResponse, PlaceholderBackendConnector}
import models.ResponseWrapper.{ErrorWrapper, SuccessWrapper}
import models.backend.{SubmitLeppRequest, SubmitLeppResponse}
import models.errors.ErrorResult.ServiceErrorResult
import models.userAnswers.LeppItemStatus.Available
import models.userAnswers.{BankAccountDetails, LeppItem, LeppSummary}
import models.{CorrelationId, ResponseWrapper}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LeppSubmissionServiceSpec extends SpecBase {
  private trait Test {
    val mockConnector: PlaceholderBackendConnector = mock[PlaceholderBackendConnector]
    val testService = new LeppSubmissionService(placeholderBackendConnector = mockConnector)

    val bankDetails: BankAccountDetails = BankAccountDetails(
      accountName = "name",
      accountNumber = "number",
      sortCode = "sortcode",
      rollNumber = Some("rollNumber")
    )

    def submitClaimResult: ConnectorResponse[SubmitLeppResponse] = testService.submitSingle(
      currentLeppLock = 1, taxYear = 2, bankDetails = bankDetails
    )

    def mockSingleClaim(result: ConnectorResponse[SubmitLeppResponse]): OngoingStubbing[ConnectorResponse[SubmitLeppResponse]] =
      when(
        mockConnector.submitLepp(
          request = any()
        )(
          hc = any(),
          ec = any(),
          cid = any()
        )
      ).thenReturn(result)

  }

  "LeppSubmissionService" - {
    "submitSingle" - {
      "should handle for a success response" in new Test {
        mockSingleClaim(
          EitherT(Future.successful(Right(
            SuccessWrapper(
              value = SubmitLeppResponse(2),
              correlationId = testCorrelationId
            )
          )))
        )
          
        val result: Either[ErrorWrapper, SuccessWrapper[SubmitLeppResponse]] = await(submitClaimResult.value)

        result mustBe a[Right[_, _]]
        result.getOrElse(
          SuccessWrapper(value = SubmitLeppResponse(99), correlationId = CorrelationId("N/A"))
        ) mustBe SuccessWrapper(
          value = SubmitLeppResponse(2),
          correlationId = testCorrelationId
        )
      }

      "should handle for an error response" in new Test {
        mockSingleClaim(
          EitherT(Future.successful(Left(
            ErrorWrapper(
              value = ServiceErrorResult(status = IM_A_TEAPOT, code = "TEAPOT_TIME"),
              correlationId = testCorrelationId
            )
          )))
        )

        val result: Either[ErrorWrapper, SuccessWrapper[SubmitLeppResponse]] = await(submitClaimResult.value)

        result mustBe a[Left[_, _]]
        result.swap.getOrElse(
          ErrorWrapper(
            value = ServiceErrorResult(status = 1, code = "N/A"),
            correlationId = CorrelationId("N/A")
          )
        ) mustBe ErrorWrapper(
          value = ServiceErrorResult(status = IM_A_TEAPOT, code = "TEAPOT_TIME"),
          correlationId = testCorrelationId
        )
      }

      "should handle for a failed future" in new Test {
        mockSingleClaim(
          EitherT(Future.failed(RuntimeException("ERROR")))
        )
        
        assertThrows[RuntimeException](
          await(submitClaimResult.value)
        )
      }
    }
    
    "submitMultiple" - {
      val leppSummary: LeppSummary = LeppSummary(
        currentLock = 1,
        items = Seq(
          LeppItem(
            taxYear = 2024,
            contributions = 100,
            taxRate = 20,
            entitlement = 20,
            status = Available
          ),
          LeppItem(
            taxYear = 2025,
            contributions = 200,
            taxRate = 20,
            entitlement = 40,
            status = Available
          )
        )
      )
      
      "handle as expected when all submissions complete successfully" in new Test {
        when(
          mockConnector.submitLepp(
            request = ArgumentMatchers.eq(
              SubmitLeppRequest(currentLowEarnersOptimisticLock = 1, taxYear = 2024, accountDetails = bankDetails)
            )
          )(
            hc = any(),
            ec = any(),
            cid = any()
          )
        ).thenReturn(
          EitherT(Future.successful(Right(
            SuccessWrapper(
              value = SubmitLeppResponse(2),
              correlationId = testCorrelationId
            )
          )))
        )
        
        when(
          mockConnector.submitLepp(
            request = ArgumentMatchers.eq(
              SubmitLeppRequest(currentLowEarnersOptimisticLock = 2, taxYear = 2025, accountDetails = bankDetails)
            )
          )(
            hc = any(),
            ec = any(),
            cid = any()
          )
        ).thenReturn(
          EitherT(Future.successful(Right(
            SuccessWrapper(
              value = SubmitLeppResponse(3),
              correlationId = testCorrelationId
            )
          )))
        )

        lazy val futureResult: ConnectorResponse[SubmitLeppResponse] = testService.submitMultiple(
          leppSummary = leppSummary,
          accountDetails = bankDetails
        )
        
        val result: Either[ErrorWrapper, SuccessWrapper[SubmitLeppResponse]] = await(futureResult.value)
        result mustBe a[Right[_, _]]
        result.getOrElse(dummySuccessResponse).value mustBe SubmitLeppResponse(3)
      }
      
      "handle as expected when a submission fails" in new Test {
        when(
          mockConnector.submitLepp(
            request = ArgumentMatchers.eq(
              SubmitLeppRequest(currentLowEarnersOptimisticLock = 1, taxYear = 2024, accountDetails = bankDetails)
            )
          )(
            hc = any(),
            ec = any(),
            cid = any()
          )
        ).thenReturn(
          EitherT(Future.successful(Right(
            SuccessWrapper(
              value = SubmitLeppResponse(2),
              correlationId = testCorrelationId
            )
          )))
        )

        when(
          mockConnector.submitLepp(
            request = ArgumentMatchers.eq(
              SubmitLeppRequest(currentLowEarnersOptimisticLock = 2, taxYear = 2025, accountDetails = bankDetails)
            )
          )(
            hc = any(),
            ec = any(),
            cid = any()
          )
        ).thenReturn(
          EitherT(Future.successful(Left(
            ErrorWrapper(
              value = ServiceErrorResult(IM_A_TEAPOT, "Teapot time"),
              correlationId = testCorrelationId
            )
          )))
        )

        lazy val futureResult: ConnectorResponse[SubmitLeppResponse] = testService.submitMultiple(
          leppSummary = leppSummary,
          accountDetails = bankDetails
        )

        val result: Either[ErrorWrapper, SuccessWrapper[SubmitLeppResponse]] = await(futureResult.value)
        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorWrapper).value mustBe ServiceErrorResult(IM_A_TEAPOT, "Teapot time")
      }
    }
  }

}
