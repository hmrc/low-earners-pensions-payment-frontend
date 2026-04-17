package controllers

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.{IntegrationSpecBase, WireMockMethods}
import config.AppConfig
import play.api.Application
import play.api.http.Status.SEE_OTHER
import play.api.http.Writeable
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import java.net.URLEncoder
import scala.concurrent.Future

trait ControllerIntegrationSpecBase extends IntegrationSpecBase with WireMockMethods {
  private val authoriseUri: String = "/auth/authorise"

  private val authRequestJson: JsValue = Json.parse(
    """
      |{
      | "authorise": [
      |   {
      |      "identifiers": [],
      |      "state": "Activated",
      |      "enrolment": "HMRC-PI"
      |   }
      | ],
      | "retrieve": [
      |   "internalId",
      |   "nino",
      |   "confidenceLevel",
      |   "authorisedEnrolments"
      | ]
      |}
    """.stripMargin
  )

  private val ptaEnrolment: JsValue = Json.parse(
    """
      |{
      | "state": "Activated",
      | "key": "HMRC-PI"
      |}
    """.stripMargin
  )
  
  def mockAuthSuccess(): StubMapping = {
    val authResponseJson: JsObject =
      Json.obj("confidenceLevel" -> 250) ++ 
        Json.obj("nino" -> validNino()) ++
        Json.obj("internalId" -> "anId" )++
        Json.obj("authorisedEnrolments" -> JsArray(Seq(ptaEnrolment)))
    
    when(method = POST, uri = authoriseUri)
      .withRequestBody(authRequestJson)
      .thenReturn(status = OK, body = authResponseJson)
  }

  private def handleForAuthError[A: Writeable](request: FakeRequest[A],
                                               error: String,
                                               expectedRedirect: String): Unit =
    s"return expected result for auth error - $error" in {
      val errorString = s"""MDTP detail=\"$error\""""
      
      when(method = POST, uri = authoriseUri)
        .withRequestBody(authRequestJson)
        .thenReturn(status = UNAUTHORIZED, headers = Map("WWW-Authenticate" -> errorString))

      lazy val application: Application = fakeApplication()

      lazy val result: Future[Result] = route(application, request).getOrElse(
        Future.failed(new RuntimeException("TEST_ERROR"))
      )

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).getOrElse("N/A") shouldBe expectedRedirect
    }

  private def handleForAuthRedirect[A: Writeable](request: FakeRequest[A])
                                                (scenarioName: String,
                                                 internalIdOpt: Option[String],
                                                 ninoOpt: Option[String],
                                                 confidenceLevel: Int,
                                                 enrolments: Seq[JsValue],
                                                 expectedRedirect: String): Unit =
    s"return expected result for scenario - $scenarioName" in {
      val authResponseJson: JsObject =
        Json.obj("confidenceLevel" -> confidenceLevel) ++
          ninoOpt.map(nino => Json.obj("nino" -> nino)).getOrElse(JsObject.empty) ++
          internalIdOpt.map(id => Json.obj("internalId" -> id)).getOrElse(JsObject.empty) ++
          Json.obj("authorisedEnrolments" -> JsArray(enrolments))

      when(method = POST, uri = authoriseUri)
        .withRequestBody(authRequestJson)
        .thenReturn(status = OK, body = authResponseJson)

      val application: Application = fakeApplication()

      lazy val result: Future[Result] = route(application, request).getOrElse(
        Future.failed(new RuntimeException("TEST_ERROR"))
      )

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).getOrElse("N/A") shouldBe expectedRedirect
    }

  def testControllerAuth[A: Writeable](request: FakeRequest[A]): Unit = {
    "authorisation" when {
      "for an authorisation error" should {
        val config: AppConfig = fakeApplication().injector.instanceOf[AppConfig]
        val loginUrl: String = config.loginUrl + "?continue=" + URLEncoder.encode(
          config.loginContinueUrl,
          "UTF-8"
        )

        Seq(
          ("InvalidBearerToken", loginUrl),
          ("InternalError", controllers.auth.routes.UnauthorisedController.onPageLoad().url)
        ).foreach((error, redirect) => handleForAuthError(request, error, redirect))
      }

      "when authorisation request succeeds" should {
        val unauthorisedUrl: String = controllers.auth.routes.UnauthorisedController.onPageLoad().url
        val ivUpliftUrl: String = fakeApplication().injector.instanceOf[AppConfig].ivUpliftUrl
        
        Seq(
          ("internalId is missing", None, Some(validNino()), 250, Seq(ptaEnrolment), unauthorisedUrl),
          ("nino is missing", Some("id"), None, 250, Seq(ptaEnrolment), unauthorisedUrl),
          ("confidenceLevel is too low", Some("id"), Some(validNino()), 50, Seq(ptaEnrolment), ivUpliftUrl),
          ("PTA enrolment is missing", Some("id"), Some(validNino()), 250, Nil, unauthorisedUrl)
        ).foreach(
          (sn, id, nino, cl, enrls, rdr) => handleForAuthRedirect(request)(sn, id, nino, cl, enrls, rdr)
        )
      } 
    }
  }
}
