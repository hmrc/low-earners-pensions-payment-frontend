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
import forms.WhatAreYourBankDetailsFormProvider
import models.bars.{BarsRequestWithMandatory, RawBarsRequest}
import models.{CorrelationId, ResponseWrapper}
import pages.WhatAreYourBankDetailsPage
import play.api.data.Form
import viewmodels.Mode
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.BarsService
import utils.{Constants, CorrelationIdOptional}
import views.html.WhatAreYourBankDetailsView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatAreYourBankDetailsController @Inject()(identify: IdentifierAction,
                                                 getData: DataRetrievalAction,
                                                 validator: BarsRequestValidator,
                                                 service: BarsService,
                                                 correlationIdHandler: CorrelationIdOptional,
                                                 formProvider: WhatAreYourBankDetailsFormProvider,
                                                 view: WhatAreYourBankDetailsView,
                                                 val controllerComponents: MessagesControllerComponents)
                                                (implicit ec: ExecutionContext)
  extends LeppBaseController(identify, getData) with I18nSupport {

  private val form: Form[BarsRequestWithMandatory] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = handle { implicit request =>
    request.userAnswers.get(WhatAreYourBankDetailsPage) match {
      case Some(value) => Future.successful(Ok(view(form.fill(value), viewModel(mode, WhatAreYourBankDetailsPage))))
      case None => Future.successful(Ok(view(form, viewModel(mode, WhatAreYourBankDetailsPage))))
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = handle { implicit request =>
    correlationIdHandler.handleCorrelationId(request)(correlationId =>
      Future.successful(Ok(""))
      /*      def result: EitherT[Future, ResponseWrapper.ErrorWrapper, Result] = for {
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
            }).merge*/
    )
  }
}
