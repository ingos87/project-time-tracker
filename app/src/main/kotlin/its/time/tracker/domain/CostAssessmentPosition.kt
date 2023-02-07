package its.time.tracker.domain

import java.time.Duration

data class CostAssessmentPosition(
    val project: String,
    val totalWorkingTime: Duration,
    val topics: Set<String>,
)