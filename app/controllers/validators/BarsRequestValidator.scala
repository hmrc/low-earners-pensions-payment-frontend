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

package controllers.validators

import com.google.inject.Singleton
import models.CorrelationId
import models.ResponseWrapper.ErrorWrapper
import models.bars.*
import models.errors.{SingleValidationError, ValidationError}

import scala.util.matching.Regex

@Singleton
class BarsRequestValidator {
  //This is a placeholder class to replicate the function that form validations will have.
  def validate(request: RawBarsRequest, correlationId: CorrelationId): Either[ErrorWrapper, ValidatedBarsRequest] = {
    def checkMandatoryFieldExists[A](fieldOpt: Option[A], path: String): Either[ValidationError, A] = fieldOpt.fold(
      Left[ValidationError, A](
        SingleValidationError(
          code = "REQUEST_MISSING_MANDATORY_FIELD",
          path = path
        )
      ).withRight
    )(field => Right(field))

    def checkStringFieldFormat(field: String, format: Regex, path: String) = if(format.matches(field)){
      Right(field)
    } else {
      Left[ValidationError, String](
        SingleValidationError(
          code = "REQUEST_FIELD_FORMAT_ERROR",
          path = path
        )
      ).withRight
    }

    val nameOrError = checkMandatoryFieldExists(request.name, "/name")
    val accountNumberOrError = checkMandatoryFieldExists(request.accountNumber, "/accountNumber")
    val sortCodeOrError = checkMandatoryFieldExists(request.sortCode, "/sortCode")

    val checkMandatoryFields: Either[ValidationError, BarsRequestWithMandatory] =
      (nameOrError, accountNumberOrError, sortCodeOrError) match {
        case (Right(name), Right(accountNo), Right(sortCode)) => Right(
          BarsRequestWithMandatory(name, accountNo, sortCode, request.rollNumber)
        )
        case _ => Left(
          Seq(nameOrError, accountNumberOrError, sortCodeOrError).collect {
            case Left(err) => err
          }.reduce((err1, err2) => err1.add(err2))
        )
      }

    val checkFieldFormats: Either[ValidationError, ValidatedBarsRequest] = checkMandatoryFields.map(req => {
      val nameOrError = checkStringFieldFormat(req.name, "^[a-zA-Z -']{1,18}$".r, "/name")
      val accountNumberOrError = checkStringFieldFormat(req.accountNumber, "^[0-9]{6,8}$".r, "/accountNumber")
      val sortCodeOrError = checkStringFieldFormat(req.sortCode, "^[0-9]{6}$".r, "/sortCode")

      val rollNumberOrError: Either[ValidationError, Option[String]] = req.rollNumber.map(
        rn => checkStringFieldFormat(rn, "^[A-Z0-9]{1,18}$".r, "/rollNumber").map(Some(_))
      ).getOrElse(Right(None))

      (nameOrError, accountNumberOrError, sortCodeOrError, rollNumberOrError) match {
        case (Right(name), Right(accountNo), Right(sortCode), Right(rollNoOpt)) => Right(
          ValidatedBarsRequest(
            BarsAccount(sortCode = sortCode, accountNumber = accountNo, rollNumber = rollNoOpt),
            BarsSubject(name = Some(name))
          )
        )
        case _ => Left(
          Seq(nameOrError, accountNumberOrError, sortCodeOrError, rollNumberOrError).collect {
            case Left(err) => err
          }.reduce((err1, err2) => err1.add(err2))
        )
      }
    }).flatten

    checkFieldFormats.fold(
      err => Left(ErrorWrapper(err.toServiceErrorResult, correlationId)),
      success => Right(success)
    )
  }
}