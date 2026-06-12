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
import connectors.{ConnectorResponse, LeppRetrievalConnector}
import models.ResponseWrapper
import models.ResponseWrapper.{ErrorWrapper, SuccessWrapper}
import models.errors.ErrorResult
import models.errors.ErrorResult.{ServiceErrorResult, notEligibleError}
import models.backend.retrieve.RetrieveLeppDetailsResponse
import models.userAnswers.LeppItemStatus.Paid
import models.userAnswers.{LeppItem, LeppSummary}
import org.mockito.Mockito.when
import org.mockito.{ArgumentMatchers, stubbing}
import org.mockito.stubbing.OngoingStubbing

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LeppRetrievalServiceSpec extends SpecBase {

  private trait Test {
    private val mockConnector: LeppRetrievalConnector = mock[LeppRetrievalConnector]
    val testService: LeppRetrievalService = new LeppRetrievalService(mockConnector)
    private type ConnectorMock =  OngoingStubbing[ConnectorResponse[RetrieveLeppDetailsResponse]]

    def mockConnectorSuccess(resp: RetrieveLeppDetailsResponse): ConnectorMock = when(
      mockConnector.retrieveLeppDetails()(
        hc = ArgumentMatchers.any(),
        ec = ArgumentMatchers.any(),
        correlationId = ArgumentMatchers.any()
      )
    ).thenReturn(
      EitherT[Future, ErrorWrapper, SuccessWrapper[RetrieveLeppDetailsResponse]](
        Future.successful(Right(SuccessWrapper(resp, testCorrelationId)))
      )
    )

    def mockConnectorFailure(err: ServiceErrorResult): ConnectorMock = when(
      mockConnector.retrieveLeppDetails()(
        hc = ArgumentMatchers.any(),
        ec = ArgumentMatchers.any(),
        correlationId = ArgumentMatchers.any()
      )
    ).thenReturn(
      EitherT[Future, ErrorWrapper, SuccessWrapper[RetrieveLeppDetailsResponse]](
        Future.successful(Left(ErrorWrapper(err, testCorrelationId)))
      )
    )
  }

  "LeppRetrievalService" - {
    type ServiceResult = Either[ErrorWrapper, SuccessWrapper[LeppSummary]]
    "when LeppRetrievalConnector returns an error response" - {
      "should map to notEligibleError if status code is NOT_FOUND" in new Test {
        val error: ServiceErrorResult = ServiceErrorResult(NOT_FOUND, "No data found")
        mockConnectorFailure(error)
        val result: ServiceResult = await(testService.retrieveLeppDetails().value)
        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorWrapper) mustBe ErrorWrapper(notEligibleError, testCorrelationId)
      }

      "should return any other error" in new Test {
        val error: ServiceErrorResult = ServiceErrorResult(IM_A_TEAPOT, "TEAPOT TIME!")
        mockConnectorFailure(error)
        val result: ServiceResult = await(testService.retrieveLeppDetails().value)
        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorWrapper) mustBe ErrorWrapper(error, testCorrelationId)
      }
    }

    "when LeppRetrievalConnector returns a success response" - {
      val dummyResponse: ResponseWrapper[RetrieveLeppDetailsResponse] = SuccessWrapper(
        value = RetrieveLeppDetailsResponse(
          currentLowEarnersOptimisticLock = 9999,
          identifier = "DUMMY",
          lowEarnersDetailsList = Nil
        ),
        correlationId = testCorrelationId
      )

      "should map to notEligibleError if LeppSummary is empty" in new Test {
        val response: RetrieveLeppDetailsResponse = RetrieveLeppDetailsResponse(
          currentLowEarnersOptimisticLock = 13,
          identifier = "NOT_USED",
          lowEarnersDetailsList = Nil
        )
        mockConnectorSuccess(response)
        val result: ServiceResult = await(testService.retrieveLeppDetails().value)
        result mustBe a[Left[_, _]]
        result.swap.getOrElse(dummyErrorWrapper) mustBe ErrorWrapper(notEligibleError, testCorrelationId)
      }

      "should return success response if LeppSummary is not empty" in new Test {
        val response: RetrieveLeppDetailsResponse = retrieveResponse
        mockConnectorSuccess(response)
        val result: ServiceResult = await(testService.retrieveLeppDetails().value)
        result mustBe a[Right[_, _]]
        result.getOrElse(dummyResponse).value mustBe LeppSummary(
          currentLock = 123,
          paidItems = Some(Seq(
            LeppItem(
              id = "P-11-1",
              taxYear = 11,
              contributions = 10.56,
              taxRate = 10.56,
              entitlement = 10.56,
              status = Paid,
              claimDate = Some(LocalDate.of(2023, 6, 27))
            )
          ))
        )
      }
    }
  }
}