package services

import base.SpecBase
import cats.data.EitherT
import connectors.{BarsConnector, ConnectorResponse}
import models.ResponseWrapper.{ErrorWrapper, SuccessWrapper}
import models.bars.statuses.{AccountExists, AccountNumberWellFormatted, NameMatches, NonStandardAccountDetails, SortCodeCheck}
import models.bars.{BarsRequest, BarsResponse}
import models.errors.ErrorResult.BarsErrorResult
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api.http.Status.INTERNAL_SERVER_ERROR

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

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
      
/*      "should handle for negative BARS response" in new Test {
        val negativeBarsResponse: BarsResponse = BarsResponse(
          accountNumberIsWellFormatted = AccountNumberWellFormatted.No,
          accountExists = AccountExists.No,
          nameMatches = NameMatches.No,
          accountName = Some("Taxwell Payer"),
          nonStandardAccountDetailsRequiredForBacs = NonStandardAccountDetails.No,
          sortCodeIsPresentOnEISCD = SortCodeCheck.No,
          sortCodeSupportsDirectDebit = SortCodeCheck.No,
          sortCodeSupportsDirectCredit = SortCodeCheck.No,
          sortCodeBankName = Some("Test"),
          iban = Some("test-iban")
        )
        
        mockBarsResponse(Future.successful(Right(SuccessWrapper(negativeBarsResponse, testCorrelationId))))
        val serviceResult: Either[ErrorWrapper, SuccessWrapper[BarsResponse]] = await(result.value)
        serviceResult mustBe a[Left[_, _]]
        serviceResult.swap.getOrElse(dummyErrorWrapper).value mustBe
          BarsErrorResult(INTERNAL_SERVER_ERROR, "BARS_RESPONSE_NEGATIVE")
      }
      
      "should handle for error BARS response" in new Test {
        val errorsBarsResponse: BarsResponse = BarsResponse(
          accountNumberIsWellFormatted = AccountNumberWellFormatted.Indeterminate,
          accountExists = AccountExists.Error,
          nameMatches = NameMatches.Error,
          accountName = Some("Taxwell Payer"),
          nonStandardAccountDetailsRequiredForBacs = NonStandardAccountDetails.No,
          sortCodeIsPresentOnEISCD = SortCodeCheck.Yes,
          sortCodeSupportsDirectDebit = SortCodeCheck.Yes,
          sortCodeSupportsDirectCredit = SortCodeCheck.Yes,
          sortCodeBankName = Some("Test"),
          iban = Some("test-iban")
        )
        
        mockBarsResponse(Future.successful(Right(SuccessWrapper(errorsBarsResponse, testCorrelationId))))
        val serviceResult: Either[ErrorWrapper, SuccessWrapper[BarsResponse]] = await(result.value)
        serviceResult mustBe a[Left[_, _]]
        serviceResult.swap.getOrElse(dummyErrorWrapper).value mustBe
          BarsErrorResult(INTERNAL_SERVER_ERROR, "ERRORS_IN_BARS_RESPONSE")
      }*/
      
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
