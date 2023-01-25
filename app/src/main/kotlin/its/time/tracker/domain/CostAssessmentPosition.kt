package its.time.tracker.domain

import java.time.Duration

data class CostAssessmentPosition(
    val bookingKey: String,
    val totalWorkingTime: Duration,
    val topics: Set<String>,
)