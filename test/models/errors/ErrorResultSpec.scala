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

package models.errors

import base.SpecBase
import models.errors.ErrorResult.{ServiceErrorResult, baseWrites}
import play.api.http.Status.{BAD_REQUEST, IM_A_TEAPOT}
import play.api.libs.json.Json

import scala.concurrent.Future

class ErrorResultSpec extends SpecBase {
  "ErrorResult" - {
    "toResult" - {
      "should return the expected result" in {
        val errorResult: ErrorResult = ServiceErrorResult(BAD_REQUEST, "A_CODE", None, None)
        val futureResult = Future.successful(errorResult.toResult)
        status(futureResult) mustBe BAD_REQUEST
        contentAsJson(futureResult).toString must include("A_CODE")
      }
    }
    
    "baseWrites" - {
      "should return the expected JSON when paths field doesn't exist" in {
        val errorResult: ErrorResult = ServiceErrorResult(BAD_REQUEST, "A_CODE", None, None)
        baseWrites(errorResult) mustBe Json.parse(
          """
            |{
            | "code": "A_CODE",
            | "source": "SERVICE"
            |}
          """.stripMargin
        )
      }

      "should return the expected JSON when paths field exists but is empty" in {
        val errorResult: ErrorResult = ServiceErrorResult(BAD_REQUEST, "A_CODE", Some(Set()), None)
        baseWrites(errorResult) mustBe Json.parse(
          """
            |{
            | "code": "A_CODE",
            | "source": "SERVICE"
            |}
          """.stripMargin
        )
      }
      
      "should return the expected JSOn when paths are populated" in {
        val errorResult: ErrorResult = ServiceErrorResult(BAD_REQUEST, "A_CODE", Some(Set("path1", "path2")), None)
        baseWrites(errorResult) mustBe Json.parse(
          """
            |{
            | "code": "A_CODE",
            | "source": "SERVICE",
            | "paths": ["path1", "path2"]
            |}
          """.stripMargin
        )
      }
    }
    
    "writes" - {
      "should handle when errors field doesn't exist" in {
        val errorResult: ErrorResult = ServiceErrorResult(BAD_REQUEST, "A_CODE", Some(Set("path1", "path2")), None)
        Json.toJson(errorResult) mustBe Json.parse(
          """
            |{
            | "code": "A_CODE",
            | "source": "SERVICE",
            | "paths": ["path1", "path2"]
            |}
          """.stripMargin
        )
      }

      "should handle when errors field is empty" in {
        val errorResult: ErrorResult = ServiceErrorResult(
          status = BAD_REQUEST,
          code = "A_CODE",
          pathsOpt = Some(Set("path1", "path2")),
          errorsOpt = Some(Seq())
        )
        
        Json.toJson(errorResult) mustBe Json.parse(
          """
            |{
            | "code": "A_CODE",
            | "source": "SERVICE",
            | "paths": ["path1", "path2"]
            |}
          """.stripMargin
        )
      }
      
      "should handle when errors field is populated" in {
        val errorResult: ErrorResult = ServiceErrorResult(
          status = BAD_REQUEST,
          code = "A_CODE",
          pathsOpt = Some(Set("path1", "path2")),
          errorsOpt = Some(Seq(
            ServiceErrorResult(status = IM_A_TEAPOT, code = "code", pathsOpt = None, errorsOpt = None)
          ))
        )

        Json.toJson(errorResult) mustBe Json.parse(
          """
            |{
            | "code": "A_CODE",
            | "source": "SERVICE",
            | "paths": ["path1", "path2"],
            | "errors": [
            |   {
            |      "code": "code",
            |      "source": "SERVICE"
            |   }
            | ]
            |}
          """.stripMargin
        )
      }
    }
  }

}
