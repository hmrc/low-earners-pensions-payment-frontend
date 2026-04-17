package common

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.client.{MappingBuilder, ResponseDefinitionBuilder, WireMock}
import com.github.tomakehurst.wiremock.matching.UrlPattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.Writes

trait WireMockMethods {
  def resetAll(): Unit = WireMock.reset()

  def when(method: HTTPMethod,
           uri: String,
           queryParams: Map[String, String] = Map.empty,
           headers: Map[String, String] = Map.empty): Mapping = {
    new Mapping(method, uri, queryParams, headers, None)
  }

  class Mapping(method: HTTPMethod,
                uri: String,
                queryParams: Map[String, String],
                headers: Map[String, String], body: Option[String]) {

    private val mapping: MappingBuilder = {
      val uriMapping = method.wireMockMapping(urlPathMatching(uri))

      val uriMappingWithQueryParams = queryParams.foldLeft(uriMapping) { case (m, (key, value)) =>
        m.withQueryParam(key, matching(value))
      }

      val uriMappingWithHeaders = headers.foldLeft(uriMappingWithQueryParams) { case (m, (key, value)) =>
        m.withHeader(key, equalTo(value))
      }

      body match {
        case Some(extractedBody) => uriMappingWithHeaders.withRequestBody(equalToJson(extractedBody))
        case None                => uriMappingWithHeaders
      }
    }

    def withRequestBody[T](body: T)(implicit writes: Writes[T]): Mapping = {
      val stringBody = writes.writes(body).toString()
      new Mapping(method, uri, queryParams, headers, Some(stringBody))
    }

    def thenReturn[T](status: Int, body: T)(implicit writes: Writes[T]): StubMapping = {
      val stringBody = writes.writes(body).toString()
      thenReturnInternal(status, Map.empty, Some(stringBody))
    }

    def thenReturn(status: Int, body: String): StubMapping = {
      thenReturnInternal(status, Map.empty, Some(body))
    }

    def thenReturn(status: Int, headers: Map[String, String] = Map.empty): StubMapping = {
      thenReturnInternal(status, headers, None)
    }

    private def thenReturnInternal(status: Int, headers: Map[String, String], body: Option[String]): StubMapping = {
      val response: ResponseDefinitionBuilder = {
        val statusResponse = aResponse().withStatus(status)
        val responseWithHeaders = headers.foldLeft(statusResponse) { case (res, (key, value)) =>
          res.withHeader(key, value)
        }
        body match {
          case Some(extractedBody) => responseWithHeaders.withBody(extractedBody)
          case None                => responseWithHeaders
        }
      }

      stubFor(mapping.willReturn(response))
    }
  }

  sealed trait HTTPMethod {
    def wireMockMapping(pattern: UrlPattern): MappingBuilder
  }

  case object POST extends HTTPMethod {
    override def wireMockMapping(pattern: UrlPattern): MappingBuilder = post(pattern)
  }

  case object GET extends HTTPMethod {
    override def wireMockMapping(pattern: UrlPattern): MappingBuilder = get(pattern)
  }
}
