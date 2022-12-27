package its.time.tracker

data class ClockEvent(
    val dateTime: String,
    val eventType: EventType,
    val topic: String,
    val bookingPosition: String,
)
