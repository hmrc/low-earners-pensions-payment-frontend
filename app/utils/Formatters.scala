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

import cats.syntax.eq.*
import play.api.i18n.Messages

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

object Formatters {

  def fullDateTime(dt: Option[Instant], messages: Messages): String = {
    val zonedDateTime = dt.getOrElse(Instant.now()).atZone(ZoneId.of("Europe/London"))
    val date          = zonedDateTime.toLocalDate
    val time          = zonedDateTime.toLocalTime
    val day           = messages(date.getDayOfWeek.toString)
     
    val dateString = s"$day ${date.getDayOfMonth.toString} ${messages(date.getMonth.toString)} ${date.getYear.toString}"
    val timeString = DateTimeFormatter
      .ofPattern("HH:mm")
      .format(time)
    
    s"$timeString ${messages("between.time.and.date")} $dateString"
  }

  def submissionDate(dt: Instant, messages: Messages): String = {
    val zonedDateTime = dt.atZone(ZoneId.of("Europe/London"))
    val date = zonedDateTime.toLocalDate
    val time = zonedDateTime.toLocalTime

    val dateString = s"${date.getDayOfMonth.toString} ${messages(date.getMonth.toString)} ${date.getYear.toString}"
    val timeString = DateTimeFormatter
      .ofPattern("HH:mm")
      .format(time)

    s"$dateString ${messages("confirmation.sub.time")} $timeString"
  }

}
