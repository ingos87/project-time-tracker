package its.time.tracker.domain

import java.time.Duration

data class CostAssessmentPosition(
    val totalWorkingTime: Duration,
    val project: String,
    val topic: String,
    val story: String,
) {
    fun getProjectKey() = "$project-$topic-$story"
}