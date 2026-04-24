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

object ErrorCodes {

  val INTERNAL_ERROR = "INTERNAL_SERVER_ERROR"
  val NOT_FOUND_ERROR = "NOT_FOUND"
  val NO_MATCH = "NO_MATCH"
  val EMPTY_DATA = "EMPTY_DATA"
  val BAD_REQUEST_ERROR = "BAD_REQUEST"
  val BARS_RETURNED_REDIRECT = "BARS_RETURNED_REDIRECT"
  val ERROR_IN_BARS_REQUEST = "ERROR_IN_BARS_REQUEST"
  val FORBIDDEN_ERROR = "FORBIDDEN"
  val COULD_NOT_ACCESS_BARS_RESOURCE = "COULD_NOT_ACCESS_BARS_RESOURCE"
  val BARS_INTERNAL_SERVER_ERROR = "BARS_INTERNAL_SERVER_ERROR"
  val BARS_RETURNED_UNEXPECTED_STATUS = "BARS_RETURNED_UNEXPECTED_STATUS"
}
