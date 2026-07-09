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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import base.{AuthSupport, IntegrationSpecBase}
import models.userAnswers.LeppItemStatus.*
import models.userAnswers.{BankAccountDetails, LeppItem, LeppSummary, UserAnswers}
import play.api.libs.json.*
import play.api.test.Helpers.*

import java.time.*
import java.time.temporal.ChronoUnit.HOURS

trait ControllerIntegrationSpecBase extends IntegrationSpecBase with AuthSupport {
  val summaryModel: LeppSummary = LeppSummary(
    currentLock = 67,
    availableItems = Some(Seq(
      LeppItem(
        id = "A-25-1",
        taxYear = 2025,
        contributions = 1000,
        taxRate = 20,
        entitlement = 200,
        status = Available,
        claimDate = None
      ),
      LeppItem(
        id = "A-26-1",
        taxYear = 2026,
        contributions = 1000,
        taxRate = 20,
        entitlement = 200,
        status = Available,
        claimDate = None
      )
    )),
    paidItems = Some(Seq(
      LeppItem(
        id = "P-25-1",
        taxYear = 2025,
        contributions = 1000,
        taxRate = 20,
        entitlement = 200,
        status = Paid,
        claimDate = None
      )
    )),
    suspendedItems = Some(Seq(
      LeppItem(
        id = "S-25-1",
        taxYear = 2025,
        contributions = 1000,
        taxRate = 20,
        entitlement = 200,
        status = Suspended,
        claimDate = None
      )
    )),
    cancelledItems = Some(Seq(
      LeppItem(
        id = "C-25-1",
        taxYear = 2025,
        contributions = 1000,
        taxRate = 20,
        entitlement = 200,
        status = Cancelled,
        claimDate = None
      )
    )
    ))

  val bankAccountDetails: BankAccountDetails = BankAccountDetails(
    accountName = "Taxwell Payer",
    accountNumber = "12345678",
    sortCode = "112233",
    rollNumber = Some("1234678")
  )

  val emptyUserAnswers = UserAnswers(
    id = "some-id",
    data = JsObject.empty,
    lastUpdated = Instant.MIN
  )

  val userAnswersWithLeppSummary: UserAnswers = emptyUserAnswers.copy(
    data = Json.obj(
      "leppSummary" -> Json.toJson(summaryModel)
    )
  )

  val initialLocalDate: LocalDate = LocalDate.parse("2020-12-25")
  val clock: Clock = Clock.fixed(initialLocalDate.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneId.of("Z"))
  val expectedLockout: Instant = Instant.now(clock).plus(24, HOURS)
  
  private val authoriseUri: String = "/auth/authorise"

  val userAnswersWithBankDetails: UserAnswers = emptyUserAnswers.copy(
    data = Json.obj(
      "leppSummary" -> Json.toJson(summaryModel),
      "bankDetails" -> Json.toJson(bankAccountDetails)
    )
  )

  val userAnswersWithExistingSubmission: UserAnswers = UserAnswers(
    id = "1",
    data = Json.obj(
      "leppSummary" -> Json.toJson(summaryModel),
      "bankDetails" -> Json.toJson(bankAccountDetails),
      "leppSubmissionSummary" -> Json.toJson(summaryModel),
      "isSubmitted" -> JsBoolean(true)
    )
  )
  
  def mockAuthSuccess(nino: String = nino): StubMapping = {
    val authResponseJson: JsObject =
      Json.obj("confidenceLevel" -> 250) ++
        Json.obj("nino" -> nino) ++
        Json.obj("internalId" -> "anId") ++
        Json.obj("authorisedEnrolments" -> JsArray(Seq(ptaEnrolment)))

    when(method = POST, uri = authoriseUri)
      .withRequestBody(authRequestJson)
      .thenReturn(status = OK, body = authResponseJson)
  }

  def mockBarsVerifyStatus(status: Int = OK,
                      response: JsObject): StubMapping = {
    
    when(method = GET, uri = "/low-earners-pensions-payment/bars/verify/status")
      .thenReturn(status = status, body = response)
  }

  def mockBarsUpdateStatus(status: Int = OK,
                           response: JsObject): StubMapping = {

    when(method = POST, uri = "/low-earners-pensions-payment/bars/verify/update")
      .withRequestBody(JsObject.empty)
      .thenReturn(status = status, body = response)
  }
}
