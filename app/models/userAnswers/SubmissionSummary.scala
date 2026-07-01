package models.userAnswers

case class SubmissionSummary(acceptedIds: Seq[String], notAcceptedIds: Seq[String] = Nil) {
  val isEmpty: Boolean = acceptedIds == Nil && notAcceptedIds == Nil
  def addAccepted(leppItemId: String): SubmissionSummary = copy(acceptedIds = leppItemId +: acceptedIds)
}

object SubmissionSummary {
  val empty = SubmissionSummary(acceptedIds = Nil, notAcceptedIds = Nil)
}
