package its.time.tracker.service.util

data class ClockEvent(
    val dateTime: String,
    val eventType: EventType,
    val topic: String,
    val bookingPosition: String,
) {
    companion object {
        fun getCsvHeaderLine(): String {
            return "dateTime;eventType;topic;bookingPosition"
        }
    }
    fun toCsvLine(): String {
        return "$dateTime;${eventType.name};$topic;$bookingPosition"
    }
}