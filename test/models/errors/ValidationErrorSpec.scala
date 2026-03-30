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
import models.errors.ErrorResult.ServiceErrorResult
import play.api.http.Status.BAD_REQUEST

class ValidationErrorSpec extends SpecBase {
  private val testSingleValidationError: SingleValidationError = SingleValidationError("TEST_CODE", Set("/field1"))
  
  private val testMultipleValidationErrors = MultipleValidationErrors(Set(
    testSingleValidationError,
    testSingleValidationError.copy(paths = Set("/field2")),
    testSingleValidationError.copy(code = "ANOTHER_CODE")
  ))
  
  "ValidationError" - {
    "errToAdd" - {
      "should combine two SingleValidationErrors with different codes" in {
        testSingleValidationError.add(testSingleValidationError.copy(code = "ANOTHER_CODE")) mustBe
          MultipleValidationErrors(Set(testSingleValidationError, testSingleValidationError.copy(code = "ANOTHER_CODE")))
      }

      "should combine two SingleValidationErrors with the same codes" in {
        testSingleValidationError.add(testSingleValidationError.copy(paths = Set("/field2"))) mustBe
          testSingleValidationError.copy(paths = Set("/field1", "/field2"))
      }

      "should combine two MultipleValidationErrors" in {
        testMultipleValidationErrors.add(
          MultipleValidationErrors(Set(testSingleValidationError.copy(code = "NEW_CODE")))
        ) mustBe MultipleValidationErrors(Set(
          testSingleValidationError.copy(paths = Set("/field1", "/field2")),
          testSingleValidationError.copy(code = "ANOTHER_CODE"),
          testSingleValidationError.copy(code = "NEW_CODE")
        ))
      }

      "should combine single and multiple error classes" in {
        val expectedResult = MultipleValidationErrors(Set(
          testSingleValidationError.copy(paths = Set("/field1", "/field2", "/field3")),
          testSingleValidationError.copy(code = "ANOTHER_CODE")
        ))
        testMultipleValidationErrors.add(testSingleValidationError.copy(paths = Set("/field3"))) mustBe expectedResult
        testSingleValidationError.copy(paths = Set("/field3")).add(testMultipleValidationErrors) mustBe expectedResult
      }
    }
  }
  
  "SingleValidationError" - {
    "addSingleError" - {
      "should return a SingleValidationError with updated paths when errors share a code" in {
        testSingleValidationError.addSingleError(SingleValidationError("TEST_CODE", Set("/field2"))) mustBe
          SingleValidationError("TEST_CODE", Set("/field1", "/field2"))
      }

      "should return a MultipleValidationErrors when errors have different codes" in {
        val errorToCombine: SingleValidationError = SingleValidationError("ANOTHER_CODE", Set("/field2"))
        testSingleValidationError.addSingleError(errorToCombine) mustBe
          MultipleValidationErrors(Set(testSingleValidationError, errorToCombine))
      }
    }
    
    "toServiceErrorResult" - {
      "should return the expected result" in {
        testSingleValidationError.toServiceErrorResult mustBe ServiceErrorResult(
          status = BAD_REQUEST,
          code = "TEST_CODE",
          pathsOpt = Some(Set("/field1"))
        )
      }
    }
  }
  
  "MultipleValidationErrors" - {
    "addErrors" - {
      "should add new errors to MultipleValidationErrors and flatten where codes match" in {
        testMultipleValidationErrors.addErrors(Set(
          testSingleValidationError,
          testSingleValidationError.copy(paths = Set("/field3")),
          testSingleValidationError.copy(code = "THIRD_CODE")
        )) mustBe MultipleValidationErrors(
          Set(
            testSingleValidationError.copy(paths = Set("/field1", "/field2", "/field3")),
            testSingleValidationError.copy(code = "ANOTHER_CODE"),
            testSingleValidationError.copy(code = "THIRD_CODE")
          )
        )
      }
    }
    
    "toServiceErrorResult" - {
      "should return the expected result" in {
        testMultipleValidationErrors.toServiceErrorResult mustBe
          ServiceErrorResult(
            status = BAD_REQUEST,
            code =  "MULTIPLE_VALIDATION_ERRORS",
            pathsOpt = None,
            errorsOpt = Some(Seq(
              ServiceErrorResult(
                status = BAD_REQUEST,
                code = "TEST_CODE",
                pathsOpt = Some(Set("/field1", "/field2"))
              ),
              ServiceErrorResult(
                status = BAD_REQUEST,
                code = "ANOTHER_CODE",
                pathsOpt = Some(Set("/field1"))
              )
            ))
          )
      }
    }
  }
}
