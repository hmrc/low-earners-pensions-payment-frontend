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

package controllers

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import controllers.actions.{DataRetrievalAction, IdentifierAction}
import controllers.validators.BarsRequestValidator
import models.bars.RawBarsRequest
import models.{CorrelationId, ResponseWrapper}
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.BarsService
import utils.{Constants, CorrelationIdOptional}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatAreYourBankDetailsController @Inject()(identify: IdentifierAction,
                                                 getData: DataRetrievalAction,
                                                 validator: BarsRequestValidator,
                                                 service: BarsService,
                                                 correlationIdHandler: CorrelationIdOptional,
                                                 val controllerComponents: MessagesControllerComponents)
                                                (implicit ec: ExecutionContext)
  extends LeppBaseController(identify, getData) with I18nSupport {

  def checkBankAccountDetails(name: Option[String],
                              accountNumber: Option[String],
                              sortCode: Option[String],
                              rollNumber: Option[String]): Action[AnyContent] = handle { implicit request =>
    correlationIdHandler.handleCorrelationId(request)(correlationId =>
      def result: EitherT[Future, ResponseWrapper.ErrorWrapper, Result] = for {
        barsRequest <- EitherT.fromEither[Future](validator.validate(
          request = RawBarsRequest(name, accountNumber, sortCode, rollNumber),
          correlationId = correlationId
        ))
        barsResult <- service.checkBankAccountDetails(barsRequest, correlationId)
      } yield {
        Ok(Json.toJson(barsResult.value)).withHeaders(
          Constants.correlationIdKey -> barsResult.correlationId
        )
      }

      result.leftMap(errorResult => {
        errorResult
          .value.toResult
          .withHeaders(
            Constants.correlationIdKey -> errorResult.correlationId
          )
      }).merge
    )
  }
}
