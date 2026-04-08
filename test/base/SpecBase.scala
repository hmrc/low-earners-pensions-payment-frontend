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
import models.{CorrelationId, ResponseWrapper}
import models.ResponseWrapper.{ErrorWrapper, SuccessWrapper}
import models.bars.*
import models.errors.ErrorResult.{BarsErrorResult, ServiceErrorResult}
import models.userAnswers.UserAnswers
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
    "microservice.services.lepp-backend.host"           -> wireMockHost,
    "microservice.services.lepp-backend.port"           -> wireMockPort
  )

  val testCorrelationId: CorrelationId = CorrelationId("some-id")
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

  val testValidatedBarsRequest: BarsRequest = BarsRequest(
    account = testBarsAccount,
    subject = BarsSubject(name = Some("Taxwell Payer"))
  )

  val testBarsResponse: BarsResponse = BarsResponse(
    accountNumberIsWellFormatted = "yes",
    accountExists = "yes",
    nameMatches = "yes",
    accountName = Some("Taxwell Payer"),
    nonStandardAccountDetailsRequiredForBacs = "no",
    sortCodeIsPresentOnEISCD = "yes",
    sortCodeSupportsDirectDebit = "yes",
    sortCodeSupportsDirectCredit = "yes",
    sortCodeBankName = Some("banky bank"),
    iban = Some("iban")
  )

  val testSuccessResponse: SuccessWrapper[BarsResponse] = SuccessWrapper[BarsResponse](
    value = testBarsResponse,
    correlationId = testCorrelationId
  )

  val dummyBarsResponse: BarsResponse = BarsResponse(
    accountNumberIsWellFormatted = "N/A",
    accountExists = "N/A",
    nameMatches = "N/A",
    accountName = Some("N/A"),
    nonStandardAccountDetailsRequiredForBacs = "N/A",
    sortCodeIsPresentOnEISCD = "N/A",
    sortCodeSupportsDirectDebit = "N/A",
    sortCodeSupportsDirectCredit = "N/A",
    sortCodeBankName = Some("N/A"),
    iban = Some("N/A")
  )

  val dummySuccessResponse: ResponseWrapper[BarsResponse] = SuccessWrapper(
    value = dummyBarsResponse,
    correlationId = testCorrelationId
  )

}
