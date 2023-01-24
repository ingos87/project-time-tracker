package its.time.tracker.upload

import its.time.tracker.domain.WorkDaySummary
import java.time.LocalDate
import java.util.*

class CostAssessmentNormalizer {

    fun normalizeWorkingTime(workDaySummaries: Map<LocalDate, WorkDaySummary>): SortedMap<LocalDate, List<WorkDaySummary>> {
        // TODO implement
        return emptyMap<LocalDate, List<WorkDaySummary>>().toSortedMap()
    }
}