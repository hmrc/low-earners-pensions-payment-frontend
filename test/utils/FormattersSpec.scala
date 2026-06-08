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
import play.api.Application
import play.api.i18n.{DefaultLangs, Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.{Instant, ZoneId}

class FormattersSpec extends SpecBase {

  val application: Application = applicationBuilder().build()
  implicit val msgApi: MessagesApi = messageApi(application)
  val msgs: Messages = messages(application)
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val languageUtils: LanguageUtils = new LanguageUtils(new DefaultLangs(), app.configuration)
  val instant: Instant = Instant.now()

  val day: String = msgs(instant.atZone(ZoneId.of("Europe/London")).getDayOfWeek.toString)
  val month: String = msgs(instant.atZone(ZoneId.of("Europe/London")).getMonth.toString)
  val hour: Int = instant.atZone(ZoneId.of("Europe/London")).getHour
  val min: Int = instant.atZone(ZoneId.of("Europe/London")).getMinute
  val year: Int = instant.atZone(ZoneId.of("Europe/London")).getYear
  val dayOfMonth: String = instant.atZone(ZoneId.of("Europe/London")).getDayOfMonth.toString
  
  "Formatters" - {
    "fullDateTime" - {
      "should return a date formatted string" in {
        val str = s"$hour:$min on $day $dayOfMonth $month $year"

        val  result = Formatters.fullDateTime(Some(instant), msgs, languageUtils)
        result mustBe str
      }
    }
  }
}