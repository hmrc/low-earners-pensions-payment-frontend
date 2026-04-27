package services

import base.SpecBase
import connectors.PlaceholderBackendConnector

class ClaimSubmissionServiceSpec extends SpecBase {
  private trait Test {
    val mockConnector: PlaceholderBackendConnector = mock[PlaceholderBackendConnector]
    val testService = new ClaimSubmissionService(placeholderBackendConnector = mockConnector)
  }

}
