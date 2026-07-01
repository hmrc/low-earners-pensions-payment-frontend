package models.userAnswers

import play.api.libs.json.{Json, OWrites}

case class SubmissionSummary(acceptedIds: Seq[String], notAcceptedIds: Seq[String] = Nil) {
  val isEmpty: Boolean = acceptedIds == Nil && notAcceptedIds == Nil
  def addAccepted(leppItemId: String): SubmissionSummary = copy(acceptedIds = leppItemId +: acceptedIds)
}

object SubmissionSummary {
  given writes: OWrites[SubmissionSummary] = Json.writes[SubmissionSummary]
  val empty = SubmissionSummary(acceptedIds = Nil, notAcceptedIds = Nil)
}
