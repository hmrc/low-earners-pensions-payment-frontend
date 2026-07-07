package controllers.actions

import models.CorrelationId
import play.api.mvc.{Headers, Request}
import utils.Constants

object ActionUtils {
  def requestWithCid[A](request: Request[A])(using cid: CorrelationId): Request[A] = {
    val requestHeadersWithCid: Headers = request.headers.replace(Constants.correlationIdKey -> cid.value)
     request.withHeaders(requestHeadersWithCid)
  }
}
