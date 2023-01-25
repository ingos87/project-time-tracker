package its.time.tracker.upload

import its.time.tracker.config.Constants
import its.time.tracker.domain.CostAssessmentPosition
import its.time.tracker.domain.ClockEvent
import its.time.tracker.domain.EventType
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.MINUTES

class ProjectTimeCalculator {

    fun calculateProjectTime(clockEvents: List<ClockEvent>, useNowAsCLockOut: Boolean = false): List<CostAssessmentPosition> {
        val topicTimes = ArrayList<Pair<String, Duration>>()
        var totalWorkingTime: Duration = Duration.ZERO

        var mostRecentClockIn: LocalDateTime? = null
        var currentClockStatus = EventType.CLOCK_OUT
        var currentTopic = ""

        clockEvents.forEach {
            if (it.eventType == EventType.CLOCK_IN) {
                if (currentClockStatus == EventType.CLOCK_IN) {
                    val taskTime = Duration.between(mostRecentClockIn, it.dateTime)
                    topicTimes.add(Pair(currentTopic, taskTime))
                    totalWorkingTime = totalWorkingTime.plus(taskTime)
                }

                currentTopic = it.topic
                mostRecentClockIn = it.dateTime
                currentClockStatus = EventType.CLOCK_IN
            }
            else if (it.eventType == EventType.CLOCK_OUT) {
                if (currentClockStatus == EventType.CLOCK_IN) {
                    val taskTime = Duration.between(mostRecentClockIn, it.dateTime)
                    topicTimes.add(Pair(currentTopic, taskTime))
                    totalWorkingTime = totalWorkingTime.plus(taskTime)
                }

                currentClockStatus = EventType.CLOCK_OUT
            }
        }

        if (currentClockStatus != EventType.CLOCK_OUT) {
            if (useNowAsCLockOut) {
                val now = LocalDateTime.now()
                val durationTillNow = Duration.ofMinutes(MINUTES.between(mostRecentClockIn, now))
                topicTimes.add(Pair(currentTopic, durationTillNow))
            }
            else if (totalWorkingTime >= Constants.MAX_WORK_DURATION_TILL_AUTO_CLOCKOUT) {
                // although, this is beyond the max hours per day, any new tasks will take at least half an hour
                val imaginaryTaskTime = Duration.ofMinutes(30)
                topicTimes.add(Pair(currentTopic, imaginaryTaskTime))
            }
            else {
                val imaginaryTaskDuration = Constants.MAX_WORK_DURATION_TILL_AUTO_CLOCKOUT - totalWorkingTime
                topicTimes.add(Pair(currentTopic, imaginaryTaskDuration))
            }
        }

        return topicTimes2BookingList(topicTimes)
    }

    private fun topicTimes2BookingList(topicTimes: ArrayList<Pair<String, Duration>>): List<CostAssessmentPosition> {
        val costAssessmentPositions = ArrayList<CostAssessmentPosition>()
        topicTimes.forEach {
            val topic = it.first
            val workingTime = it.second
            val bookingKey = BookingPositionResolver.resolveTopicToBookingPosition(topic)

            val presentItem = costAssessmentPositions.find { item -> item.bookingKey == bookingKey }
            if (presentItem != null) {
                costAssessmentPositions.remove(presentItem)
                val newItem = CostAssessmentPosition(
                    bookingKey = bookingKey,
                    totalWorkingTime = presentItem.totalWorkingTime.plus(workingTime),
                    topics = presentItem.topics.plus(topic)
                )
                costAssessmentPositions.add(newItem)
            }
            else {
                val newItem = CostAssessmentPosition(
                    bookingKey = bookingKey,
                    totalWorkingTime = workingTime,
                    topics = setOf(topic)
                )
                costAssessmentPositions.add(newItem)
            }
        }

        return costAssessmentPositions
    }
}
