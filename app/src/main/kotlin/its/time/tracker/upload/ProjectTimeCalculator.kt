package its.time.tracker.upload

import its.time.tracker.config.Constants
import its.time.tracker.domain.CostAssessmentPosition
import its.time.tracker.domain.ClockEvent
import its.time.tracker.domain.EventType
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.MINUTES

class ProjectTimeCalculator {

    fun calculateProjectTime(clockEvents: List<ClockEvent>, useNowAsClockOut: Boolean = false): List<CostAssessmentPosition> {
        val singleCostAssessments = collectSingleProjectTimes(clockEvents, useNowAsClockOut)
        return unifyPositions(singleCostAssessments)
    }

    private fun collectSingleProjectTimes(clockEvents: List<ClockEvent>, useNowAsClockOut: Boolean): List<CostAssessmentPosition> {
        val singleCostAssessments = mutableListOf<CostAssessmentPosition>()
        var totalWorkingTime: Duration = Duration.ZERO

        var mostRecentClockIn: LocalDateTime? = null
        var currentClockStatus = EventType.CLOCK_OUT
        var currentProject = ""
        var currentTopic = ""

        clockEvents.forEach {
            if (it.eventType == EventType.CLOCK_IN) {
                if (currentClockStatus == EventType.CLOCK_IN) {
                    val taskTime = Duration.between(mostRecentClockIn, it.dateTime)
                    singleCostAssessments.add(CostAssessmentPosition(currentProject, taskTime, setOf(currentTopic)))
                    totalWorkingTime = totalWorkingTime.plus(taskTime)
                }

                currentProject = Constants.COST_ASSESSMENT_SETUP.getOfficialProjectName(it.project)
                currentTopic = it.topic
                mostRecentClockIn = it.dateTime
                currentClockStatus = EventType.CLOCK_IN
            }
            else if (it.eventType == EventType.CLOCK_OUT) {
                if (currentClockStatus == EventType.CLOCK_IN) {
                    val taskTime = Duration.between(mostRecentClockIn, it.dateTime)
                    singleCostAssessments.add(CostAssessmentPosition(currentProject, taskTime, setOf(currentTopic)))
                    totalWorkingTime = totalWorkingTime.plus(taskTime)
                }

                currentClockStatus = EventType.CLOCK_OUT
            }
        }

        if (currentClockStatus != EventType.CLOCK_OUT) {
            if (useNowAsClockOut) {
                val now = LocalDateTime.now()
                val durationTillNow = Duration.ofMinutes(MINUTES.between(mostRecentClockIn, now))
                singleCostAssessments.add(CostAssessmentPosition(currentProject, durationTillNow, setOf(currentTopic)))
            }
            else if (totalWorkingTime >= Constants.MAX_WORK_DURATION_TILL_AUTO_CLOCKOUT) {
                // although, this is beyond the max hours per day, any new tasks will take at least half an hour
                val imaginaryTaskTime = Duration.ofMinutes(30)
                singleCostAssessments.add(CostAssessmentPosition(currentProject, imaginaryTaskTime, setOf(currentTopic)))
            }
            else {
                val imaginaryTaskDuration = Constants.MAX_WORK_DURATION_TILL_AUTO_CLOCKOUT - totalWorkingTime
                singleCostAssessments.add(CostAssessmentPosition(currentProject, imaginaryTaskDuration, setOf(currentTopic)))
            }
        }

        return singleCostAssessments.toList()
    }

    private fun unifyPositions(positions: List<CostAssessmentPosition>): List<CostAssessmentPosition> {
        val groupedPositions = positions.groupBy { it.project }

        val unifiedPositions = groupedPositions.map { (_, positions) ->
            val totalWorkingTime = positions.fold(Duration.ZERO) { acc, position -> acc.plus(position.totalWorkingTime) }
            val topics = positions.flatMap { it.topics }.toSet()
            CostAssessmentPosition(positions.first().project, totalWorkingTime, topics)
        }

        return unifiedPositions
    }
}
