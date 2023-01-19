package its.time.tracker.domain

import its.time.tracker.service.util.DateTimeUtil
import java.time.LocalDateTime

data class ClockEvent(
    val dateTime: LocalDateTime,
    val eventType: EventType,
    val topic: String,
) {
    companion object {
        fun getCsvHeaderLine(): String {
            return "dateTime;eventType;topic"
        }
    }
    fun toCsvLine(): String {
        return "${DateTimeUtil.temporalToString(dateTime)};${eventType.name};$topic"
    }
}
