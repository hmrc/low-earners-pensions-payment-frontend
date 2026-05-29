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

import cats.data.EitherT
import com.google.inject.{Inject, Singleton}
import connectors.{ConnectorResponse, LeppRetrievalConnector}
import models.ResponseWrapper.ErrorWrapper
import models.errors.ErrorResult
import models.errors.ErrorResult.{ServiceErrorResult, notEligibleError}
import play.api.http.Status.*
import models.userAnswers.LeppSummary
import models.{CorrelationId, ResponseWrapper}
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LeppRetrievalService @Inject()(connector: LeppRetrievalConnector) {
  def retrieveLeppDetails(nino: Nino)
                         (implicit hc: HeaderCarrier,
                          ec: ExecutionContext,
                          cid: CorrelationId): ConnectorResponse[LeppSummary] = {
    val summaryResult: ConnectorResponse[LeppSummary] = for {
      wrappedResponse <- connector.retrieveLeppDetails(nino)
      leppSummary = wrappedResponse.map(LeppSummary(_))
    } yield leppSummary
    
    summaryResult.biflatMap(
      err => err.value match {
        case ServiceErrorResult(status, _, _, _) if status == NOT_FOUND =>
          EitherT(Future.successful(Left(err.copy(value = notEligibleError))))
        case _ =>
          summaryResult
      },
      success => if(success.value.isNonEmpty) {
        summaryResult
      } else {
        EitherT(Future.successful(Left(ErrorWrapper(notEligibleError, success.correlationId))))
      }
    )
  }
}
