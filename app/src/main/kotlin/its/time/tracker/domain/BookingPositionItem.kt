package its.time.tracker.domain

import java.time.Duration

data class BookingPositionItem(
    val bookingKey: String,
    val totalWorkingTime: Duration,
    val topics: Set<String>,
)