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
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, ZoneId, ZonedDateTime}

class FormattersSpec extends SpecBase {

  val application: Application = applicationBuilder().build()
  implicit val msgApi: MessagesApi = messageApi(application)
  val msgs: Messages = messages(application)
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val instant: Instant = Instant.now()

  val zonedDateTime: ZonedDateTime = instant.atZone(ZoneId.of("Europe/London"))
  val date: LocalDate = zonedDateTime.toLocalDate
  val day: String = msgs(date.getDayOfWeek.toString)
  val month: String = msgs(date.getMonth.toString)

  val time: String = DateTimeFormatter
    .ofPattern("HH:mm")
    .format(zonedDateTime.toLocalTime)
  
  val year: String = date.getYear.toString
  val dayOfMonth: String = date.getDayOfMonth.toString
  
  "Formatters" - {
    "fullDateTime" - {
      "should return a date formatted string" in {
        val str = s"$time on $day $dayOfMonth $month $year"

        val  result = Formatters.fullDateTime(Some(instant), msgs)
        result mustBe str
      }
    }
    
    "submissionDate" - {
      "should return a date formatted string" in {
        val str = s"$dayOfMonth $month $year at $time"

        val result = Formatters.submissionDate(instant, msgs)
        result mustBe str
      }
    }
  }
}