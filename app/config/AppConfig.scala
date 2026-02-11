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

package config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.ConfidenceLevel.L250
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import play.api.i18n.Lang

@Singleton
class AppConfig @Inject()(config: Configuration):

  private def loadConfig(key: String): String = config.get[String](key)

  //Application config
  val host: String = loadConfig("host")
  val appName: String = loadConfig("appName")
  private val servicesConfig = ServicesConfig(config)

  // Feedback config
  val exitSurveyUrl: String = loadConfig("urls.signOutWithFeedback")

  //URLs
  val loginUrl: String = loadConfig("urls.login")
  val loginContinueUrl: String = loadConfig("urls.loginContinue")
  lazy val signOutUrl: String = loadConfig("urls.signOutWithFeedback")

  //IV uplift config
  val confidenceLevelMinimum: ConfidenceLevel =
    ConfidenceLevel
      .fromInt(config.get[Int]("confidenceLevelMinimum"))
      .getOrElse(L250)

  private lazy val ivUpliftBaseUrl: String = servicesConfig.baseUrl("identity-verification-frontend")
  private val ivOrigin = "low-earners-pensions-payment"
  private val ivSuccessUrl: String = loginContinueUrl
  private val ivFailureUrl: String = loadConfig("urls.ivUpliftFailureUrl")

  lazy val ivUpliftUrl: String =
    s"$ivUpliftBaseUrl/mdtp/uplift" +
      s"?origin=$ivOrigin" +
      s"&confidenceLevel=$confidenceLevelMinimum" +
      s"&completionURL=$ivSuccessUrl" +
      s"&failureURL=$ivFailureUrl"

  //Timeout config
  val timeout: Int = config.get[Int]("timeout-dialog.timeout")
  val countdown: Int = config.get[Int]("timeout-dialog.countdown")

  //MongoDB config
  val sessionDataTtl: Long = config.get[Int]("mongodb.sessionDataTtl")
  val encryptionKey: String = config.get[String]("mongodb.encryption.key")
  val useEncryption: Boolean = config.get[Boolean]("mongodb.encryption.enabled")

  //Language config
  def languageMap: Map[String, Lang] =
    config
      .get[Seq[String]]("play.i18n.langs")
      .map(lang => lang -> Lang(lang)).toMap

  val welshLanguageSupportEnabled: Boolean =
    config.getOptional[Boolean]("features.welsh-language-support").getOrElse(false)

  //Beta feedback config
  val contactFrontendUrl: String = s"${loadConfig("urls.betaFeedbackUrl")}/?service=low-earners-pensions-payment"