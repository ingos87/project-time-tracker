package its.time.tracker.domain

import its.time.tracker.util.DateTimeUtil
import java.time.LocalDateTime

data class ClockEvent(
    val dateTime: LocalDateTime,
    val eventType: EventType,
    val project: String,
    val topic: String,
    val story: String,
) {
    companion object {
        fun getCsvHeaderLine(): String {
            return "dateTime;eventType;project;topic;story"
        }
    }
    fun toCsvLine(): String {
        return "${DateTimeUtil.temporalToString(dateTime)};${eventType.name};$project;$topic;$story"
    }
}
