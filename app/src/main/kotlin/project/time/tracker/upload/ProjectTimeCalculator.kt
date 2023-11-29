package project.time.tracker.upload

import project.time.tracker.config.Constants
import project.time.tracker.domain.CostAssessmentPosition
import project.time.tracker.domain.ClockEvent
import project.time.tracker.domain.EventType
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.MINUTES

class ProjectTimeCalculator {

    companion object {
        fun unifyPositions(allPositions: List<CostAssessmentPosition>): List<CostAssessmentPosition> {
            val grouped = allPositions.groupBy { "${it.project}-${it.topic}-${it.story}" }

            val unifiedPositions = mutableListOf<CostAssessmentPosition>()
            grouped.forEach { (_, positions) ->
                val totalDuration = positions.fold(Duration.ZERO) { total, position ->
                    total.plus(position.totalWorkingTime)
                }
                unifiedPositions.add(CostAssessmentPosition(totalDuration, positions[0].project, positions[0].topic, positions[0].story))
            }

            return unifiedPositions
        }
    }

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
        var currentStory = ""

        clockEvents.forEach {
            if (it.eventType == EventType.CLOCK_IN) {
                if (currentClockStatus == EventType.CLOCK_IN) {
                    val taskTime = Duration.between(mostRecentClockIn, it.dateTime)
                    singleCostAssessments.add(CostAssessmentPosition(
                        taskTime,
                        currentProject,
                        currentTopic,
                        currentStory
                    ))
                    totalWorkingTime = totalWorkingTime.plus(taskTime)
                }

                currentProject = Constants.COST_ASSESSMENT_SETUP.getOfficialProjectName(it.project)
                mostRecentClockIn = it.dateTime
                currentTopic = it.topic
                currentStory = it.story
                currentClockStatus = EventType.CLOCK_IN
            }
            else if (it.eventType == EventType.CLOCK_OUT) {
                if (currentClockStatus == EventType.CLOCK_IN) {
                    val taskTime = Duration.between(mostRecentClockIn, it.dateTime)
                    singleCostAssessments.add(CostAssessmentPosition(
                        taskTime,
                        currentProject,
                        currentTopic,
                        currentStory
                    ))
                    totalWorkingTime = totalWorkingTime.plus(taskTime)
                }

                currentClockStatus = EventType.CLOCK_OUT
            }
        }

        if (currentClockStatus != EventType.CLOCK_OUT) {
            if (useNowAsClockOut) {
                val now = LocalDateTime.now()
                val durationTillNow = Duration.ofMinutes(MINUTES.between(mostRecentClockIn, now))
                singleCostAssessments.add(CostAssessmentPosition(
                    durationTillNow,
                    currentProject,
                    currentTopic,
                    currentStory
                ))
            }
            else if (totalWorkingTime >= Constants.MAX_WORK_DURATION_TILL_AUTO_CLOCKOUT) {
                // although, this is beyond the max hours per day, any new tasks will take at least half an hour
                val imaginaryTaskTime = Duration.ofMinutes(30)
                singleCostAssessments.add(CostAssessmentPosition(
                    imaginaryTaskTime,
                    currentProject,
                    currentTopic,
                    currentStory
                ))
            }
            else {
                val imaginaryTaskDuration = Constants.MAX_WORK_DURATION_TILL_AUTO_CLOCKOUT - totalWorkingTime
                singleCostAssessments.add(CostAssessmentPosition(
                    imaginaryTaskDuration,
                    currentProject,
                    currentTopic,
                    currentStory
                ))
            }
        }

        return singleCostAssessments.toList()
    }
}
