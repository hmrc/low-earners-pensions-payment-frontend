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

package controllers.actions

import base.SpecBase
import connectors.BarsVerifyStatusConnector
import models.barsLockout.{BarsVerifyStatusResponse, NumberOfBarsVerifyAttempts}
import models.requests.{AuthUser, BarsVerifiedRequest, IdentifierRequest}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import play.api.mvc.*
import play.api.mvc.Results.Redirect
import play.api.test.{FakeHeaders, FakeRequest}
import uk.gov.hmrc.domain.Nino
import utils.CorrelationIdHandler

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class BarsLockoutActionSpec extends SpecBase with Results {

  val headers: FakeHeaders = FakeHeaders(Seq("x-conversation-id" -> "X-id"))
  
  private object TestBarsLockoutAction extends BarsLockoutAction {
    override val barsVerifyStatusConnector: BarsVerifyStatusConnector = mock[BarsVerifyStatusConnector]
    override val correlationIdHandler: CorrelationIdHandler = mock[CorrelationIdHandler]

    override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, BarsVerifiedRequest[A]]] = {
      Future.successful(Left(ImATeapot("")))
    }

    override protected def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  }
  
  "BarsLockoutAction" - {
    "handleWithBarsStatus" - {
      "must return InternalServerError when request to verify bars status fails" in {
        when(
          TestBarsLockoutAction.barsVerifyStatusConnector.status()(
            ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()
          )
        ).thenReturn(
          Future.failed(new RuntimeException(""))
        )
        
        val result: Either[Result, BarsVerifiedRequest[Nothing]] = await(
          TestBarsLockoutAction.handleWithBarsStatus(f = _ => Left(ImATeapot("")))
        )
        
        result mustBe a[Left[_, _]]
        val expectedResult: Result = InternalServerError
        result.swap.getOrElse(NotFound("")) mustBe expectedResult
      }

      "must return expected result when request to verify bars status succeeds" in {
        when(
          TestBarsLockoutAction.barsVerifyStatusConnector.status()(
            ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()
          )
        ).thenReturn(
          Future.successful(BarsVerifyStatusResponse(
            attempts = NumberOfBarsVerifyAttempts.zero,
            lockoutExpiryDateTime = None
          ))
        )

        val result: Either[Result, BarsVerifiedRequest[Nothing]] = await(
          TestBarsLockoutAction.handleWithBarsStatus(f = _ => Left(ImATeapot("")))
        )

        result mustBe a[Left[_, _]]
        val expectedResult: Result = ImATeapot("")
        result.swap.getOrElse(NotFound("")) mustBe expectedResult
      }
    }
  }
  
  "NoRedirectBarsLockoutAction" - {
    trait Test {
      val barsVerifyStatusConnector: BarsVerifyStatusConnector = mock[BarsVerifyStatusConnector]
      val correlationIdHandler: CorrelationIdHandler = mock[CorrelationIdHandler]
      
      when(correlationIdHandler.getCorrelationId(ArgumentMatchers.any())).thenReturn(testCorrelationId)
      
      val idRequest: IdentifierRequest[AnyContentAsEmpty.type] = IdentifierRequest(
        request = FakeRequest(),
        user = AuthUser(userId = "id", nino = Nino("AA123456C"), itmpNameOpt = None)
      )
      
      val action: NoRedirectBarsLockoutAction = new NoRedirectBarsLockoutAction(
        barsVerifyStatusConnector,
        correlationIdHandler
      )
    }
    
    "should return lockout data when a user has BARS lockout" in new Test {
      when(
        barsVerifyStatusConnector.status()(
          ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()
        )
      ).thenReturn(
        Future.successful(BarsVerifyStatusResponse(
          attempts = NumberOfBarsVerifyAttempts(2),
          lockoutExpiryDateTime = Some(Instant.ofEpochMilli(1000))
        ))
      )
      
      lazy val result: Either[Result, BarsVerifiedRequest[AnyContentAsEmpty.type]] = await(
        action.refine(idRequest)
      )
      
      val dummyRequest: BarsVerifiedRequest[AnyContentAsEmpty.type] = BarsVerifiedRequest(idRequest, None)
      
      result mustBe a[Right[_, _]]
      result.getOrElse(dummyRequest).barsLockoutExpiryOpt mustBe Some(Instant.ofEpochMilli(1000))
    }
    
    "should return no lockout data for a user who isn't locked out from BARS" in new Test {
      when(
        barsVerifyStatusConnector.status()(
          ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()
        )
      ).thenReturn(
        Future.successful(BarsVerifyStatusResponse(
          attempts = NumberOfBarsVerifyAttempts.zero,
          lockoutExpiryDateTime = None
        ))
      )

      lazy val result: Either[Result, BarsVerifiedRequest[AnyContentAsEmpty.type]] = await(
        action.refine(idRequest)
      )

      val dummyRequest: BarsVerifiedRequest[AnyContentAsEmpty.type] = BarsVerifiedRequest(idRequest, None)

      result mustBe a[Right[_, _]]
      result.getOrElse(dummyRequest).barsLockoutExpiryOpt mustBe None
    }
  }

  "RedirectBarsLockoutAction" - {
    trait Test {
      val barsVerifyStatusConnector: BarsVerifyStatusConnector = mock[BarsVerifyStatusConnector]
      val correlationIdHandler: CorrelationIdHandler = mock[CorrelationIdHandler]

      when(correlationIdHandler.getCorrelationId(ArgumentMatchers.any())).thenReturn(testCorrelationId)

      val idRequest: IdentifierRequest[AnyContentAsEmpty.type] = IdentifierRequest(
        request = FakeRequest(),
        user = AuthUser(userId = "id", nino = Nino("AA123456C"), itmpNameOpt = None)
      )

      val action: RedirectBarsLockoutAction = new RedirectBarsLockoutAction(
        barsVerifyStatusConnector,
        correlationIdHandler
      )
    }
    
    "refine" - {
      "must redirect to BarsLockoutController when the user locked out" in new Test {
        when(
          barsVerifyStatusConnector.status()(
            hc = ArgumentMatchers.any(), 
            ec = ArgumentMatchers.any(), 
            correlationId = ArgumentMatchers.any()
          )
        ).thenReturn(
          Future.successful(
            BarsVerifyStatusResponse(
              attempts = NumberOfBarsVerifyAttempts(3),
              lockoutExpiryDateTime = Some(Instant.now())
            )
          )
        )
        
        val result: Either[Result, BarsVerifiedRequest[AnyContentAsEmpty.type]] = await(action.refine(idRequest))
        result mustBe a[Left[_, _]]
        result.swap.getOrElse(InternalServerError("")) mustBe Redirect(controllers.bars.routes.BarsLockoutController.onPageLoad())
      }

      "must return a Bars request when the user not lockout" in new Test {
        when(
          barsVerifyStatusConnector.status()(
            hc = ArgumentMatchers.any(), 
            ec = ArgumentMatchers.any(), 
            correlationId = ArgumentMatchers.any()
          )
        ).thenReturn(
          Future.successful(
            BarsVerifyStatusResponse(
              attempts = NumberOfBarsVerifyAttempts(1),
              lockoutExpiryDateTime = None
            )
          )
        )

        val dummyRequest: BarsVerifiedRequest[AnyContentAsEmpty.type] = BarsVerifiedRequest(idRequest, None)
        
        val result: Either[Result, BarsVerifiedRequest[AnyContentAsEmpty.type]] = await(action.refine(idRequest))
        result mustBe a[Right[_, _]]
        result.getOrElse(dummyRequest).barsLockoutExpiryOpt mustBe None
      } 
    }
  }
}