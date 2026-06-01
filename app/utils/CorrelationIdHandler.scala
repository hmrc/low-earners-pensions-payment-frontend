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

import com.google.inject.Singleton
 import models.CorrelationId
 import models.errors.ErrorResult.ServiceErrorResult
 import play.api.http.Status.BAD_REQUEST
 import play.api.mvc.{Request, Result}

 import java.util.UUID
 import scala.concurrent.Future

@Singleton
class CorrelationIdHandler {
  protected[utils] def generateCorrelationId: CorrelationId = CorrelationId(UUID.randomUUID().toString)
  
  def getCorrelationId[A](request: Request[A]): CorrelationId =
    request.headers.get(Constants.correlationIdKey) match {
    case Some(value) => CorrelationId(value)
    case _ => generateCorrelationId
  }
}
