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

import models.errors.ErrorResult.ServiceErrorResult
import play.api.http.Status.BAD_REQUEST

sealed trait ValidationError {
  val code: String
  val status: Int = BAD_REQUEST

  def toServiceErrorResult: ServiceErrorResult

  def add(errToAdd: ValidationError): ValidationError = (this, errToAdd) match {
    case (single1: SingleValidationError, single2: SingleValidationError) =>
      single1.addSingleError(single2)
    case (multiple1: MultipleValidationErrors, multiple2: MultipleValidationErrors) =>
      multiple1.addErrors(multiple2.errs)
    case (multiple: MultipleValidationErrors, single: SingleValidationError) =>
      multiple.addErrors(Set(single))
    case (single: SingleValidationError, multiple: MultipleValidationErrors) =>
      multiple.addErrors(Set(single))
  }
}

case class SingleValidationError(code: String, paths: Set[String]) extends ValidationError {
  protected[errors] def addSingleError(toAdd: SingleValidationError): ValidationError =
    if (code == toAdd.code) {
      this.copy(paths = paths ++ toAdd.paths)
    } else {
      MultipleValidationErrors(this, toAdd)
    }

  def toServiceErrorResult: ServiceErrorResult = ServiceErrorResult(
    status = status,
    code = code,
    pathsOpt = Some(paths)
  )
}

object SingleValidationError {
  def apply(code: String, path: String): SingleValidationError = SingleValidationError(code, Set(path))
}

case class MultipleValidationErrors(errs: Set[SingleValidationError]) extends ValidationError {
  override val code: String = "MULTIPLE_VALIDATION_ERRORS"

  private def withFlattenedErrors: MultipleValidationErrors = {
    MultipleValidationErrors(
      errs.groupBy(_.code)
        .map((code, errs) => SingleValidationError(code, errs.flatMap(_.paths)))
        .toSet
    )
  }

  protected[errors] def addErrors(errsToAdd: Set[SingleValidationError]): MultipleValidationErrors = {
    val combinedErrs: Set[SingleValidationError] = errs ++ errsToAdd
    MultipleValidationErrors(combinedErrs).withFlattenedErrors
  }

  def toServiceErrorResult = ServiceErrorResult(
    status = status,
    code = code,
    errorsOpt = Some(withFlattenedErrors.errs.map(_.toServiceErrorResult).toSeq)
  )
}

object MultipleValidationErrors {
  def apply(err1: SingleValidationError, err2: SingleValidationError): MultipleValidationErrors =
    MultipleValidationErrors(Set(err1, err2))
}