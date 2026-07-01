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
import connectors.{AcceptLeppPaymentConnector, ConnectorResponse, rawConnectorFailure, rawConnectorSuccess}
import models.ResponseWrapper.{ErrorWrapper, SuccessWrapper}
import models.backend.accept.{AcceptLeppPaymentRequest, AcceptLeppPaymentRequestBody, AcceptLeppPaymentResponse}
import models.errors.ErrorResult
import models.errors.ErrorResult.{BackendErrorResult, ServiceErrorResult, leppSubmissionError}
import models.requests.{AuthUser, DataRequest}
import models.userAnswers.LeppItemStatus.{Available, Cancelled, Paid, Suspended}
import models.userAnswers.{BankAccountDetails, LeppItem, LeppSummary, SubmissionSummary}
import models.{CorrelationId, ResponseWrapper}
import org.mockito.{ArgumentMatchers, Mockito}
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.{calls, verify, when}
import org.mockito.stubbing.OngoingStubbing
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpException
import utils.Constants

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LeppSubmissionServiceSpec extends SpecBase {
  private trait Test {
    val mockConnector: AcceptLeppPaymentConnector = mock[AcceptLeppPaymentConnector]
    val mockAuditService: AuditService = mock[AuditService]
    val testService: LeppSubmissionService = new LeppSubmissionService(
      connector = mockConnector,
      auditService = mockAuditService
    )

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
    
    def acceptPaymentResult: ConnectorResponse[AcceptLeppPaymentResponse] = testService.acceptPayment(
      request = acceptRequest,
      entitlement = 1234.56
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
    "acceptPayment" - {
      "should handle for a success response and submit success audit" in new Test {
        mockSingleClaim(
          result = EitherT(Future.successful(Right(
            SuccessWrapper(value = AcceptLeppPaymentResponse(2), correlationId = testCorrelationId)
          )))
        )
        
        val result: Either[ErrorWrapper, SuccessWrapper[AcceptLeppPaymentResponse]] = await(acceptPaymentResult.value)
        
        result mustBe a[Right[_, _]]
        result.getOrElse(dummyResult) mustBe SuccessWrapper(
          value = AcceptLeppPaymentResponse(2),
          correlationId = testCorrelationId
        )
        
        verify(mockAuditService).auditSubmissionSuccess(
          nino = ArgumentMatchers.any(),
          bankAccountDetails = ArgumentMatchers.any(),
          taxYear = ArgumentMatchers.any(),
          entitlement = ArgumentMatchers.any()
        )(using ArgumentMatchers.any(), ArgumentMatchers.any())
      }

      "should handle for an error response" in new Test {
        val errorResult: ErrorWrapper = ErrorWrapper(
          value = BackendErrorResult(status = IM_A_TEAPOT, code = "TEAPOT_TIME"),
          correlationId = testCorrelationId
        )
        
        mockSingleClaim(result = EitherT(Future.successful(Left(errorResult))))

        val result: Either[ErrorWrapper, SuccessWrapper[AcceptLeppPaymentResponse]] = await(acceptPaymentResult.value)

        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorResult) mustBe errorResult
        verify(mockAuditService).auditSubmissionFailure(
          nino = ArgumentMatchers.any(),
          bankAccountDetails = ArgumentMatchers.any(),
          taxYear = ArgumentMatchers.any(),
          entitlement = ArgumentMatchers.any(),
          wasSkipped = ArgumentMatchers.any()
        )(using ArgumentMatchers.any(), ArgumentMatchers.any())
      }

      "should handle when result contains an exception" in new Test {
        mockSingleClaim(
          EitherT(Future.failed(RuntimeException("ERROR")))
        )

        assertThrows[RuntimeException](
          await(acceptPaymentResult.value)
        )
      }
  }
    
    "acceptMultiplePayments" - {
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
            id = "A-25-2",
            taxYear = 2025,
            contributions = 1000,
            taxRate = 20,
            entitlement = 200,
            status = Available,
            claimDate = None
          ),
          LeppItem(
            id = "A-25-3",
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

        val req3: AcceptLeppPaymentRequest = req1.copy(
          taxYear = 2025,
          body = requestBody.copy(currentLowEarnersOptimisticLock = 69)
        )

        when(
          mockConnector.acceptPayment(request = ArgumentMatchers.eq(req3)
          )(
            hc = any(),
            ec = any(),
            cid = any()
          )
        ).thenReturn(
          EitherT(Future.successful(Right(
            SuccessWrapper(
              value = AcceptLeppPaymentResponse(70),
              correlationId = testCorrelationId
            )
          )))
        )

        lazy val futureResult: ConnectorResponse[SubmissionSummary] = testService.acceptMultiplePayments(
          nino = nino,
          leppSummary = leppSummary,
          accountDetails = bankDetails
        )
        
        val expectedResult = SubmissionSummary(acceptedIds = Seq("A-25-3", "A-25-2", "A-25-1"))
        val result: Either[ErrorWrapper, SuccessWrapper[SubmissionSummary]] = await(futureResult.value)
        
        result mustBe a[Right[_, _]]
        result.getOrElse(SuccessWrapper(SubmissionSummary.empty, testCorrelationId)).value mustBe expectedResult
        verify(mockAuditService, Mockito.atLeast(3)).auditSubmissionSuccess(
          nino = ArgumentMatchers.any(),
          bankAccountDetails = ArgumentMatchers.any(),
          taxYear = ArgumentMatchers.any(),
          entitlement = ArgumentMatchers.any()
        )(using ArgumentMatchers.any(), ArgumentMatchers.any())
      }

      "handle as expected when a submission fails, not including the first" in new Test {
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

        lazy val futureResult: ConnectorResponse[SubmissionSummary] = testService.acceptMultiplePayments(
          nino = nino,
          leppSummary = leppSummary,
          accountDetails = bankDetails
        )
        
        val dummySummaryResult: SuccessWrapper[SubmissionSummary] = SuccessWrapper(
          value = SubmissionSummary.empty,
          correlationId = testCorrelationId
        )

        val result: Either[ErrorWrapper, SuccessWrapper[SubmissionSummary]] = await(futureResult.value)
        result mustBe a[Right[_, _]]
        result.getOrElse(dummySummaryResult).value mustBe SubmissionSummary(Seq("A-25-1"), Seq("A-25-2", "A-25-3"))
        verify(mockAuditService, Mockito.atMost(2)).auditSubmissionSuccess(
          nino = ArgumentMatchers.any(),
          bankAccountDetails = ArgumentMatchers.any(),
          taxYear = ArgumentMatchers.any(),
          entitlement = ArgumentMatchers.any()
        )(using ArgumentMatchers.any(), ArgumentMatchers.any())
      }

      "handle as expected when the first submission fails" in new Test {
        when(
          mockConnector.acceptPayment(
            request = ArgumentMatchers.any()
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
        
        lazy val futureResult: ConnectorResponse[SubmissionSummary] = testService.acceptMultiplePayments(
          nino = nino,
          leppSummary = leppSummary,
          accountDetails = bankDetails
        )

        val result: Either[ErrorWrapper, SuccessWrapper[SubmissionSummary]] = await(futureResult.value)
        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorResult).value mustBe leppSubmissionError
        verify(mockAuditService, Mockito.never()).auditSubmissionSuccess(
          nino = ArgumentMatchers.any(),
          bankAccountDetails = ArgumentMatchers.any(),
          taxYear = ArgumentMatchers.any(),
          entitlement = ArgumentMatchers.any()
        )(using ArgumentMatchers.any(), ArgumentMatchers.any())
      }

    }
    
    "resultWithCid" - {
      "should handle correctly for a success containing a valid CID" in new Test {
        given cid: CorrelationId = CorrelationId("1234")
        
        val result: Either[ErrorWrapper, SuccessWrapper[AcceptLeppPaymentResponse]] = await(
          testService.resultWithCid(
            result = rawConnectorSuccess(AcceptLeppPaymentResponse(123))
          ).value
        )
        
        result mustBe a[Right[_, _]]
        result.getOrElse(dummyResult).correlationId.value mustBe "1234"
      }

      "should handle correctly for an error containing a valid CID" in new Test {
        given cid: CorrelationId = CorrelationId("1234")

        val result: Either[ErrorWrapper, SuccessWrapper[AcceptLeppPaymentResponse]] = await(
          testService.resultWithCid(
            result = rawConnectorFailure(ServiceErrorResult(IM_A_TEAPOT, "TEAPOT_TIME"))
          ).value
        )

        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorResult).correlationId.value mustBe "1234"
      }

      "should handle correctly for a success containing an invalid CID" in new Test {
        given cid: CorrelationId = CorrelationId("1234")

        val result: Either[ErrorWrapper, SuccessWrapper[AcceptLeppPaymentResponse]] = await(
          testService.resultWithCid(
            result = EitherT(Future.successful(Right(SuccessWrapper(
              value = AcceptLeppPaymentResponse(123),
              correlationId = CorrelationId(Constants.noCorrelationIdString)
            ))))
          ).value
        )

        result mustBe a[Right[_, _]]
        result.getOrElse(dummyResult).correlationId.value mustBe "1234"
      }

      "should handle correctly for an error containing an invalid CID" in new Test {
        given cid: CorrelationId = CorrelationId("1234")

        val result: Either[ErrorWrapper, SuccessWrapper[AcceptLeppPaymentResponse]] = await(
          testService.resultWithCid(
            result = EitherT(Future.successful(Left(ErrorWrapper(
              value = ServiceErrorResult(IM_A_TEAPOT, "TEAPOT_TIME"),
              correlationId = CorrelationId(Constants.noCorrelationIdString)
            ))))
          ).value
        )

        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorResult).correlationId.value mustBe "1234"
      }
    }
  }
}
