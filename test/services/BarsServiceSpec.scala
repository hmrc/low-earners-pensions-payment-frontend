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
import connectors.{BarsConnector, ConnectorResponse}
import models.ResponseWrapper.{ErrorWrapper, SuccessWrapper}
import models.bars.BarsResponse
import models.bars.statuses.{AccountExists, AccountNumberWellFormatted, DirectCreditSupported, NameMatches, NonStandardAccountDetails, SortCodeExists}
import models.errors.ErrorResult
import models.errors.ErrorResult.BarsErrorResult
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BarsServiceSpec extends SpecBase {
  
  private trait Test {
    private val mockConnector: BarsConnector = mock[BarsConnector]
    private val testService: BarsService = new BarsService(mockConnector)
    
    def mockBarsResponse(
                          resp: Future[Either[ErrorWrapper, SuccessWrapper[BarsResponse]]]
                        ): OngoingStubbing[ConnectorResponse[BarsResponse]] = when(
      mockConnector.checkBankAccountDetails(
        request = ArgumentMatchers.any(),
        correlationId = ArgumentMatchers.any()
      )(
        hc = ArgumentMatchers.any(),
        ec = ArgumentMatchers.any()
      )
    ).thenReturn(EitherT(resp))
    
    lazy val result: ConnectorResponse[BarsResponse] = testService.checkBankAccountDetails(
      barsRequest = testBarsRequest,
      correlationId = testCorrelationId
    )
  }
  
  "BarsService" - {
    "checkBankAccountDetails" - {
      "should handle for successful BARS response" in new Test {
        mockBarsResponse(Future.successful(Right(SuccessWrapper(testBarsResponse, testCorrelationId))))
        val serviceResult: Either[ErrorWrapper, SuccessWrapper[BarsResponse]] = await(result.value) 
        serviceResult mustBe a[Right[_, _]]
        serviceResult.getOrElse(dummySuccessResponse).value mustBe testBarsResponse
      }
      
      "should handle for a successful BARS response with request errors" in new Test {
        val resp: BarsResponse = BarsResponse(
          accountNumberIsWellFormatted = AccountNumberWellFormatted.No,
          accountExists = AccountExists.Inapplicable,
          nameMatches = NameMatches.Inapplicable,
          accountName = Some("Taxwell Payer"),
          nonStandardAccountDetailsRequiredForBacs = NonStandardAccountDetails.Yes,
          sortCodeIsPresentOnEISCD = SortCodeExists.Yes,
          sortCodeSupportsDirectDebit = "yes",
          sortCodeSupportsDirectCredit = DirectCreditSupported.Yes,
          sortCodeBankName = Some("banky bank"),
          iban = Some("iban")
        )
        
        mockBarsResponse(Future.successful(Right(SuccessWrapper(resp, testCorrelationId))))
        val serviceResult: Either[ErrorWrapper, SuccessWrapper[BarsResponse]] = await(result.value)
        serviceResult mustBe a[Left[_, _]]
        
        val err: ErrorResult = serviceResult.swap.getOrElse(dummyErrorWrapper).value
        err.status mustBe BAD_REQUEST
        err.code mustBe "BARS_REQUEST_ERRORS"
        val expectedCodes: Seq[String] = Seq("FAILED_MODULUS_CHECK", "ADDITIONAL_INFORMATION_REQUIRED")
        err.errorsOpt.getOrElse(Nil).map(_.code) mustBe expectedCodes
      }
      
      "should handle for a successful BARS response with failed check errors" in new Test {
        val resp: BarsResponse = BarsResponse(
          accountNumberIsWellFormatted = AccountNumberWellFormatted.Yes,
          accountExists = AccountExists.Yes,
          nameMatches = NameMatches.Error,
          accountName = Some("Taxwell Payer"),
          nonStandardAccountDetailsRequiredForBacs = NonStandardAccountDetails.No,
          sortCodeIsPresentOnEISCD = SortCodeExists.Yes,
          sortCodeSupportsDirectDebit = "yes",
          sortCodeSupportsDirectCredit = DirectCreditSupported.Yes,
          sortCodeBankName = Some("banky bank"),
          iban = Some("iban")
        )

        mockBarsResponse(Future.successful(Right(SuccessWrapper(resp, testCorrelationId))))
        val serviceResult: Either[ErrorWrapper, SuccessWrapper[BarsResponse]] = await(result.value)
        serviceResult mustBe a[Left[_, _]]
        
        val err: ErrorResult = serviceResult.swap.getOrElse(dummyErrorWrapper).value
        err mustBe BarsErrorResult(INTERNAL_SERVER_ERROR, "NAME_MATCHES_ERROR")
      }
      
      "should handle for successful BARS response with both error types" in new Test {
        val resp: BarsResponse = BarsResponse(
          accountNumberIsWellFormatted = AccountNumberWellFormatted.No,
          accountExists = AccountExists.Indeterminate,
          nameMatches = NameMatches.Indeterminate,
          accountName = Some("Taxwell Payer"),
          nonStandardAccountDetailsRequiredForBacs = NonStandardAccountDetails.Yes,
          sortCodeIsPresentOnEISCD = SortCodeExists.Error,
          sortCodeSupportsDirectDebit = "yes",
          sortCodeSupportsDirectCredit = DirectCreditSupported.No,
          sortCodeBankName = Some("banky bank"),
          iban = Some("iban")
        )

        mockBarsResponse(Future.successful(Right(SuccessWrapper(resp, testCorrelationId))))
        val serviceResult: Either[ErrorWrapper, SuccessWrapper[BarsResponse]] = await(result.value)
        serviceResult mustBe a[Left[_, _]]
        
        val err: ErrorResult = serviceResult.swap.getOrElse(dummyErrorWrapper).value
        err.status mustBe INTERNAL_SERVER_ERROR
        err.code mustBe "BARS_CHECK_FAILED"
        val expectedCodes: Seq[String] = Seq(
          "FAILED_MODULUS_CHECK",
          "ACCOUNT_EXISTS_INDETERMINATE",
          "NAME_MATCHES_INDETERMINATE",
          "ADDITIONAL_INFORMATION_REQUIRED",
          "SORT_CODE_EXISTS_ERROR",
          "DIRECT_CREDIT_UNSUPPORTED"
        )
        err.errorsOpt.getOrElse(Nil).map(_.code) mustBe expectedCodes
      }
      
      "should handle for failed BARS response" in new Test {
        mockBarsResponse(Future.successful(Left(ErrorWrapper(
          BarsErrorResult(IM_A_TEAPOT, "TEAPOT_TIME"), testCorrelationId)
        )))
        val serviceResult: Either[ErrorWrapper, SuccessWrapper[BarsResponse]] = await(result.value)
        serviceResult mustBe a[Left[_, _]]
        serviceResult.swap.getOrElse(dummyErrorWrapper).value mustBe 
          BarsErrorResult(IM_A_TEAPOT, "TEAPOT_TIME")
      }
    }
  }

}
