package its.time.tracker.service.util

import its.time.tracker.Constants
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.MINUTES

class ProjectTimeCalculator {

    fun calculateProjectTime(clockEvents: List<ClockEvent>, useNowAsCLockOut: Boolean = false): List<BookingPositionItem> {
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
            else if (totalWorkingTime >= Constants.MAX_WORK_DURATION_PER_DAY) {
                // although, this is beyond the max hours per day, any new tasks will take at least half an hour
                val imaginaryTaskTime = Duration.ofMinutes(30)
                topicTimes.add(Pair(currentTopic, imaginaryTaskTime))
            }
            else {
                val imaginaryTaskDuration = Constants.MAX_WORK_DURATION_PER_DAY - totalWorkingTime
                topicTimes.add(Pair(currentTopic, imaginaryTaskDuration))
            }
        }

        return topicTimes2BookingList(topicTimes)
    }

    private fun topicTimes2BookingList(topicTimes: ArrayList<Pair<String, Duration>>): List<BookingPositionItem> {
        val bookingPositionItems = ArrayList<BookingPositionItem>()
        topicTimes.forEach {
            val topic = it.first
            val workingTime = it.second
            val bookingKey = BookingPositionResolver.resolveTopicToBookingPosition(topic)

            val presentItem = bookingPositionItems.find { item -> item.bookingKey == bookingKey }
            if (presentItem != null) {
                bookingPositionItems.remove(presentItem)
                val newItem = BookingPositionItem(
                    bookingKey = bookingKey,
                    totalWorkingTime = presentItem.totalWorkingTime.plus(workingTime),
                    topics = presentItem.topics.plus(topic)
                )
                bookingPositionItems.add(newItem)
            }
            else {
                val newItem = BookingPositionItem(
                    bookingKey = bookingKey,
                    totalWorkingTime = workingTime,
                    topics = setOf(topic)
                )
                bookingPositionItems.add(newItem)
            }
        }

        return bookingPositionItems
    }
}

data class BookingPositionItem(
    val bookingKey: String,
    val totalWorkingTime: Duration,
    val topics: Set<String>,
)