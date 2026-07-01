package models.userAnswers

case class SubmissionSummary(acceptedIds: Seq[String], notAcceptedIds: Seq[String] = Nil) {
  val isEmpty: Boolean = this == SubmissionSummary.empty
  def addAccepted(leppItemId: String): SubmissionSummary = copy(acceptedIds = leppItemId +: acceptedIds)
}

object SubmissionSummary {
  val empty = SubmissionSummary(acceptedIds = Nil, notAcceptedIds = Nil)
}
