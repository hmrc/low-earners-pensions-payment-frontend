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
import connectors.{AcceptLeppPaymentConnector, ConnectorResponse}
import models.ResponseWrapper.{ErrorWrapper, SuccessWrapper}
import models.backend.accept.{AcceptLeppPaymentRequest, AcceptLeppPaymentRequestBody, AcceptLeppPaymentResponse}
import models.errors.ErrorResult
import models.errors.ErrorResult.{BackendErrorResult, ServiceErrorResult}
import models.userAnswers.LeppItemStatus.{Available, Cancelled, Paid, Suspended}
import models.userAnswers.{BankAccountDetails, LeppItem, LeppSummary}
import models.{CorrelationId, ResponseWrapper}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import uk.gov.hmrc.domain.Nino

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LeppSubmissionServiceSpec extends SpecBase {
  private trait Test {
    val mockConnector: AcceptLeppPaymentConnector = mock[AcceptLeppPaymentConnector]
    val testService: LeppSubmissionService = new LeppSubmissionService(connector = mockConnector)

    val bankDetails: BankAccountDetails = BankAccountDetails(
      accountName = "name",
      accountNumber = "number",
      sortCode = "sortcode",
      rollNumber = Some("rollNumber")
    )
    
    val nino: Nino = Nino(generateNino())
    
    val requestBody: AcceptLeppPaymentRequestBody = AcceptLeppPaymentRequestBody(
      currentLowEarnersOptimisticLock = 67,
      lowEarnersAccountDetails = bankDetails
    )
    
    val acceptRequest: AcceptLeppPaymentRequest = AcceptLeppPaymentRequest(
      identifier = nino,
      taxYear = 2024,
      body = requestBody
    )
    
    val dummyResult: ResponseWrapper[AcceptLeppPaymentResponse] = SuccessWrapper(
      value = AcceptLeppPaymentResponse(99),
      correlationId = CorrelationId("N/A")
    )
    
    val dummyErrorResult: ResponseWrapper[ErrorResult] = ErrorWrapper(
      value = BackendErrorResult(status = 1, code = "N/A"),
      correlationId = CorrelationId("N/A")
    )
    
    def submitClaimResult: ConnectorResponse[AcceptLeppPaymentResponse] = testService.submitSingle(
      acceptLeppPaymentRequest = acceptRequest
    )

    def mockSingleClaim(
                         result: ConnectorResponse[AcceptLeppPaymentResponse]
                       ): OngoingStubbing[ConnectorResponse[AcceptLeppPaymentResponse]] =
      when(
        mockConnector.acceptPayment(request = any())(
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
          result = EitherT(Future.successful(Right(
            SuccessWrapper(value = AcceptLeppPaymentResponse(2), correlationId = testCorrelationId)
          )))
        )

        val result: Either[ErrorWrapper, SuccessWrapper[AcceptLeppPaymentResponse]] = await(submitClaimResult.value)

        result mustBe a[Right[_, _]]
        result.getOrElse(dummyResult) mustBe SuccessWrapper(
          value = AcceptLeppPaymentResponse(2),
          correlationId = testCorrelationId
        )
      }

      "should handle for an error response" in new Test {
        mockSingleClaim(
          result = EitherT(Future.successful(Left(
            ErrorWrapper(
              value = BackendErrorResult(status = IM_A_TEAPOT, code = "TEAPOT_TIME"),
              correlationId = testCorrelationId
            )
          )))
        )

        val result: Either[ErrorWrapper, SuccessWrapper[AcceptLeppPaymentResponse]] = await(submitClaimResult.value)

        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorResult) mustBe ErrorWrapper(
          value = BackendErrorResult(status = IM_A_TEAPOT, code = "TEAPOT_TIME"),
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
        currentLock = 67,
        availableItems = Some(Seq(
          LeppItem(
            id = "A-25-1",
            taxYear = 2024,
            contributions = 1000,
            taxRate = 20,
            entitlement = 200,
            status = Available,
            claimDate = None
          ),
          LeppItem(
            id = "A-25-1",
            taxYear = 2025,
            contributions = 1000,
            taxRate = 20,
            entitlement = 200,
            status = Available,
            claimDate = None
          )
        )),
        paidItems = Some(Seq(
          LeppItem(
            id = "P-25-1",
            taxYear = 2025,
            contributions = 1000,
            taxRate = 20,
            entitlement = 200,
            status = Paid,
            claimDate = None
          )
        )),
        suspendedItems = Some(Seq(
          LeppItem(
            id = "S-25-1",
            taxYear = 2025,
            contributions = 1000,
            taxRate = 20,
            entitlement = 200,
            status = Suspended,
            claimDate = None
          )
        )),
        cancelledItems = Some(Seq(
          LeppItem(
            id = "C-25-1",
            taxYear = 2025,
            contributions = 1000,
            taxRate = 20,
            entitlement = 200,
            status = Cancelled,
            claimDate = None
          )
        ))
      )

      "handle as expected when all submissions complete successfully" in new Test {
        val req1: AcceptLeppPaymentRequest = acceptRequest

        when(
          mockConnector.acceptPayment(
            request = ArgumentMatchers.eq(req1)
          )(
            hc = any(),
            ec = any(),
            cid = any()
          )
        ).thenReturn(
          EitherT(Future.successful(Right(
            SuccessWrapper(
              value = AcceptLeppPaymentResponse(BigInt(68)),
              correlationId = testCorrelationId
            )
          )))
        )

        val req2: AcceptLeppPaymentRequest = req1.copy(
          taxYear = 2025,
          body = requestBody.copy(currentLowEarnersOptimisticLock = 68)
        )

        when(
          mockConnector.acceptPayment(request = ArgumentMatchers.eq(req2)
          )(
            hc = any(),
            ec = any(),
            cid = any()
          )
        ).thenReturn(
          EitherT(Future.successful(Right(
            SuccessWrapper(
              value = AcceptLeppPaymentResponse(69),
              correlationId = testCorrelationId
            )
          )))
        )

        lazy val futureResult: ConnectorResponse[AcceptLeppPaymentResponse] = testService.submitMultiple(
          nino = nino,
          bankAccountDetails = bankDetails,
          leppSummary = leppSummary
        )

        val result: Either[ErrorWrapper, SuccessWrapper[AcceptLeppPaymentResponse]] = await(futureResult.value)
        result mustBe a[Right[_, _]]
        result.getOrElse(dummySuccessResponse).value mustBe AcceptLeppPaymentResponse(69)
      }

      "handle as expected when a submission fails" in new Test {
        when(
          mockConnector.acceptPayment(
            request = ArgumentMatchers.any()
          )(
            hc = any(),
            ec = any(),
            cid = any()
          )
        ).thenReturn(
          EitherT(Future.successful(Right(
            SuccessWrapper(
              value = AcceptLeppPaymentResponse(68),
              correlationId = testCorrelationId
            )
          )))
        )

        when(
          mockConnector.acceptPayment(
            request = ArgumentMatchers.eq(
              acceptRequest.copy(
                taxYear = 2025,
                body = requestBody.copy(currentLowEarnersOptimisticLock = 68)
              )
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

        lazy val futureResult: ConnectorResponse[AcceptLeppPaymentResponse] = testService.submitMultiple(
          nino = nino,
          bankAccountDetails = bankDetails,
          leppSummary = leppSummary
        )

        val result: Either[ErrorWrapper, SuccessWrapper[AcceptLeppPaymentResponse]] = await(futureResult.value)
        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorWrapper).value mustBe ServiceErrorResult(IM_A_TEAPOT, "Teapot time")
      }
    }
  }
}
