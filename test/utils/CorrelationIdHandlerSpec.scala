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

package utils

import base.SpecBase
import models.CorrelationId
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest

import scala.concurrent.Future

class CorrelationIdHandlerSpec extends SpecBase {
  "CorrelationIdHandler" - {
    "handle" - {
      val block: CorrelationId => Future[Result] = id => Future.successful(Ok(id.value))
      
      def idToRequest(idOpt: Option[String]) = idOpt.fold(FakeRequest())(
        id => FakeRequest().withHeaders("correlationId" -> id)
      )
      
      "should invoke block when correlation ID exists and is mandatory" in {
        val result: Future[Result] = new CorrelationIdMandatory().handleCorrelationId(idToRequest(Some("id")))(block)
        status(result) mustBe OK
        contentAsString(result) must include("id")
      }

      "should invoke block when correlation ID exists and is optional" in {
        val result: Future[Result] = new CorrelationIdOptional().handleCorrelationId(idToRequest(Some("id")))(block)
        status(result) mustBe OK
        contentAsString(result) must include("id")
      }

      "should return error when correlation ID is required and doesn't exist" in {
        val result: Future[Result] = new CorrelationIdMandatory().handleCorrelationId(idToRequest(None))(block)
        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include("CORRELATION_ID_HEADER_MISSING")
      }

      "should generate ID and invoke block when correlation ID is optional and doesn't exist" in {
        val handler: CorrelationIdOptional =  new CorrelationIdOptional() {
          override protected[utils] def generateCorrelationId: CorrelationId = CorrelationId("generatedId")
        }
        
        val result: Future[Result] = handler.handleCorrelationId(idToRequest(None))(block)
        status(result) mustBe OK
        contentAsString(result) must include("generatedId")
      }
    }
  }
}
