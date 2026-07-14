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

package base

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.*
import play.api.test.Helpers.*

import scala.util.Random

trait AuthSupport extends WireMockMethods {
  val authoriseUri: String = "/auth/authorise"
  val nino: String = validNino()

  val authRequestJson: JsValue = Json.parse(
    """
      |{
      | "authorise": [
      |   {
      |      "identifiers": [],
      |      "state": "Activated",
      |      "enrolment": "HMRC-PT"
      |   }
      | ],
      | "retrieve": [
      |   "internalId",
      |   "nino",
      |   "confidenceLevel",
      |   "authorisedEnrolments",
      |   "optionalItmpName"
      | ]
      |}
    """.stripMargin
  )

  val ptaEnrolment: JsValue = Json.parse(
    """
      |{
      | "state": "Activated",
      | "key": "HMRC-PT"
      |}
    """.stripMargin
  )

  def validNino(prefix: String = "AA"): String = {
    val num = Random.nextInt(1000000)
    val suffix = "A"
    val str: String = Random.alphanumeric.filter(_.isLetter).take(2).map(_.toUpper).mkString

    prefix + f"$str$num%06d$suffix".drop(prefix.length)
  }

  def mockAuthSuccess(): StubMapping = {
    val authResponseJson: JsObject =
      Json.obj("confidenceLevel" -> 250) ++
        Json.obj("nino" -> nino) ++
        Json.obj("internalId" -> "anId") ++
        Json.obj("authorisedEnrolments" -> JsArray(Seq(ptaEnrolment))) ++
        Json.obj("optionalItmpName" -> Json.obj("givenName" -> JsString("Name")))

    when(method = POST, uri = authoriseUri)
      .withRequestBody(authRequestJson)
      .thenReturn(status = OK, body = authResponseJson)
  }

}
