package models.bars.statuses

trait BarsStatus {
 val errorScenario: BarsStatus
 def isErrorScenario(status: BarsStatus): Boolean = status == errorScenario
}
