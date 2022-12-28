package its.time.tracker.service.util

import its.time.tracker.service.util.DateTimeUtil.Companion.addTimes
import its.time.tracker.service.util.DateTimeUtil.Companion.extractTimeFromDateTime
import its.time.tracker.service.util.DateTimeUtil.Companion.getTimeDiff

class ProjectTimeCalculator {

    fun calculateProjectTime(clockEvents: List<ClockEvent>): List<BookingPositionItem> {
        val topicTimes = ArrayList<Pair<String, String>>()

        var mostRecentClockIn = ""
        var currentClockStatus = EventType.CLOCK_OUT
        var currentTopic = ""

        clockEvents.forEach {
            if (it.eventType == EventType.CLOCK_IN) {
                if (currentClockStatus == EventType.CLOCK_IN) {
                    val workTime = getTimeDiff(
                        extractTimeFromDateTime(mostRecentClockIn),
                        extractTimeFromDateTime(it.dateTime)
                    )
                    topicTimes.add(Pair(currentTopic, workTime))
                }

                currentTopic = it.topic
                mostRecentClockIn = it.dateTime
                currentClockStatus = EventType.CLOCK_IN
            }
            else if (it.eventType == EventType.CLOCK_OUT) {
                if (currentClockStatus == EventType.CLOCK_IN) {
                    val workTime = getTimeDiff(
                        extractTimeFromDateTime(mostRecentClockIn),
                        extractTimeFromDateTime(it.dateTime)
                    )
                    topicTimes.add(Pair(currentTopic, workTime))
                }

                currentClockStatus = EventType.CLOCK_OUT
            }
        }

        if (currentClockStatus != EventType.CLOCK_OUT) {
            println("No final clock-out found")
            // TODO implement something to end the work day
        }

        return topicTimes2BookingList(topicTimes)
    }

    private fun topicTimes2BookingList(topicTimes: ArrayList<Pair<String, String>>): List<BookingPositionItem> {
        val bookingPositionItems = ArrayList<BookingPositionItem>()
        topicTimes.forEach {
            val topic = it.first
            val workTime = it.second
            val bookingKey = BookingPositionResolver.resolveTopicToBookingPosition(topic)

            val presentItem = bookingPositionItems.find { item -> item.bookingKey == bookingKey }
            if (presentItem != null) {
                bookingPositionItems.remove(presentItem)
                val newItem = BookingPositionItem(
                    bookingKey = bookingKey,
                    totalWorkTime = addTimes(presentItem.totalWorkTime, workTime),
                    topics = presentItem.topics.plus(topic)
                )
                bookingPositionItems.add(newItem)
            }
            else {
                val newItem = BookingPositionItem(
                    bookingKey = bookingKey,
                    totalWorkTime = workTime,
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
    val totalWorkTime: String,
    val topics: Set<String>,
)