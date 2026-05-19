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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import controllers.actions.*
import models.ResponseWrapper.{ErrorWrapper, SuccessWrapper}
import models.bars.*
import models.bars.statuses.*
import models.errors.ErrorResult.{BarsErrorResult, ServiceErrorResult}
import models.nps.*
import models.nps.ClaimStatus.Paid
import models.userAnswers.UserAnswers
import models.{CorrelationId, ResponseWrapper}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.http.{HeaderNames, Status}
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.*
import play.api.test.Helpers.running
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits, ResultExtractors}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}

import java.net.URLEncoder
import scala.reflect.ClassTag

trait SpecBase
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience
    with MockitoSugar
    with BeforeAndAfterEach
    with WireMockSupport
    with HttpClientV2Support
    with GuiceOneServerPerSuite
    with BeforeAndAfterAll
    with FutureAwaits
    with DefaultAwaitTimeout
    with HeaderNames
    with Status
    with ResultExtractors {

  val server: WireMockServer = new WireMockServer(wireMockConfig().dynamicPort())

  val userAnswersId: String = "id"

  def emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId)

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  val fakeIdentifierAction: FakeIdentifierAction = new FakeIdentifierAction()

  protected def applicationBuilder(userAnswers: UserAnswers = emptyUserAnswers,
                                   identifierAction: IdentifierAction = fakeIdentifierAction,
                                   servicesConfig: Map[String, Any] = servicesConfig): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(servicesConfig)
      .overrides(
        bind[IdentifierAction].toInstance(identifierAction),
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers))
      )

  def runningApplication(block: Application => Unit): Unit =
    running(_ => applicationBuilder(emptyUserAnswers))(block)

  protected def injected[A: ClassTag](implicit app: Application): A = app.injector.instanceOf[A]

  def urlEncode(input: String): String = URLEncoder.encode(input, "utf-8")

  val servicesConfig: Map[String, Any] = Map(
    "microservice.services.lepp-backend.host" -> wireMockHost,
    "microservice.services.lepp-backend.port" -> wireMockPort
  )

  implicit val testCorrelationId: CorrelationId = CorrelationId("some-id")
  implicit val dummyHeaderCarrier: HeaderCarrier = HeaderCarrier()

  val dummyErrorWrapper: ErrorWrapper = ErrorWrapper(
    value = ServiceErrorResult(IM_A_TEAPOT, "FOOBAR"),
    correlationId = testCorrelationId
  )

  val testServiceErrorWrapper: ErrorWrapper = ErrorWrapper(
    value = ServiceErrorResult(IM_A_TEAPOT, "TEST_ERROR"),
    correlationId = testCorrelationId
  )

  val testDownstreamErrorWrapper: ErrorWrapper = ErrorWrapper(
    value = BarsErrorResult(IM_A_TEAPOT, "TEST_ERROR"),
    correlationId = testCorrelationId
  )

  val testBarsAccount: BarsAccount = BarsAccount(
    sortCode = "112233",
    accountNumber = "12345678",
    rollNumber = Some("rollNumber")
  )

  val testBarsSubject: BarsSubject = BarsSubject(
    title = Some("Mr"),
    name = Some("Taxwell Payer"),
    firstName = Some("Taxwell"),
    lastName = Some("Payer")
  )

  val dummyValidatedBarsRequest: BarsRequest = BarsRequest(
    account = BarsAccount(
      sortCode = "N/A",
      accountNumber = "N/A",
      rollNumber = None
    ),
    subject = BarsSubject(
      title = None,
      name = Some("N/A"),
      firstName = None,
      lastName = None
    )
  )

  val testBarsRequest: BarsRequest = BarsRequest(
    account = testBarsAccount,
    subject = BarsSubject(name = Some("Taxwell Payer"))
  )

  val testBarsResponse: BarsResponse = BarsResponse(
    accountNumberIsWellFormatted = AccountNumberWellFormatted.Yes,
    accountExists = AccountExists.Yes,
    nameMatches = NameMatches.Yes,
    accountName = Some("Taxwell Payer"),
    nonStandardAccountDetailsRequiredForBacs = NonStandardAccountDetails.No,
    sortCodeIsPresentOnEISCD = SortCodeExists.Yes,
    sortCodeSupportsDirectDebit = "yes",
    sortCodeSupportsDirectCredit = DirectCreditSupported.Yes,
    sortCodeBankName = Some("banky bank"),
    iban = Some("iban")
  )

  val testSuccessResponse: SuccessWrapper[BarsResponse] = SuccessWrapper[BarsResponse](
    value = testBarsResponse,
    correlationId = testCorrelationId
  )

  val dummyBarsResponse: BarsResponse = BarsResponse(
    accountNumberIsWellFormatted = AccountNumberWellFormatted.No,
    accountExists = AccountExists.No,
    nameMatches = NameMatches.No,
    accountName = Some("N/A"),
    nonStandardAccountDetailsRequiredForBacs = NonStandardAccountDetails.No,
    sortCodeIsPresentOnEISCD = SortCodeExists.No,
    sortCodeSupportsDirectDebit = "no",
    sortCodeSupportsDirectCredit = DirectCreditSupported.No,
    sortCodeBankName = Some("N/A"),
    iban = Some("N/A")
  )

  val dummySuccessResponse: ResponseWrapper[BarsResponse] = SuccessWrapper(
    value = dummyBarsResponse,
    correlationId = testCorrelationId
  )
  
  private val dataDetails: LowEarnersDataDetails = LowEarnersDataDetails(
    responseTimestamp = Some("2023-06-27 09:12:28"),
    calculationSequenceNumber = 123,
    dataSourceMaster = "CESA",
    netPayContributionsTotal = Some(10.56),
    basicRatePercentage = Some(10.56),
    totalAllowances = Some(10.56),
    totalIncome = Some(10.56),
    totalDeductions = Some(10.56),
    totalTaxDue = Some(10.56)
  )

  private val claimDetails: LowEarnersClaimDetails = LowEarnersClaimDetails(
    claimSequenceNumber = 123,
    entitlementAmount = Some(10.56),
    claimStatus = Paid,
    inSelfAssessment = true,
    calculationDate = Some("2023-06-27"),
    claimDate = Some("2023-06-27"),
    reminderOutputSent = true,
    reissueClaimOutput = true,
    originalAmount = Some(10.56)
  )

  val calculation: LowEarnersCalculation = LowEarnersCalculation(
    lowEarnersClaimDetails = claimDetails,
    lowEarnersDataDetails = dataDetails
  )

  private val details: LowEarnersDetails = LowEarnersDetails(
    taxYear = 11,
    lowEarnersCalculations = Seq(calculation)
  )

  val retrieveResponse: RetrieveClaimsResponse = RetrieveClaimsResponse(
    currentLowEarnersOptimisticLock = 123,
    identifier = "id",
    lowEarnersDetailsList = Seq(details)
  )

  val leppResponse: ResponseWrapper[RetrieveClaimsResponse] = SuccessWrapper(
    value = retrieveResponse,
    correlationId = testCorrelationId
  )

  def enumReadsTest[E: Reads](jsonValue: String, modelValue: E): Unit =
    s"should read correctly for a value of: $jsonValue" in {
      val json: JsString = JsString(jsonValue)
      json.validate[E] mustBe a[JsSuccess[_]]
      json.as[E] mustBe modelValue
    }

  def enumWritesTest[E: Writes](modelValue: E, jsonString: String): Unit =
    s"should write correctly for a value of: ${modelValue.toString}" in {
      Json.toJson(modelValue) mustBe JsString(jsonString)
    }

}
