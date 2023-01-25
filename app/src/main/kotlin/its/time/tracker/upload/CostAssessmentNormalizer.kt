package its.time.tracker.upload

import its.time.tracker.domain.CostAssessmentPosition
import its.time.tracker.domain.WorkDaySummaryCollection
import java.time.LocalDate
import java.util.*

class CostAssessmentNormalizer {

    fun normalizeWorkingTime(workDaySummaries: WorkDaySummaryCollection): SortedMap<LocalDate, List<CostAssessmentPosition>> {
        val compliantWorkDaySummaries = CostAssessmentValidator().moveProjectTimesToValidDays(workDaySummaries)
        val roundedProjectTimes = CostAssessmentRoundingService().roundProjectTimes(compliantWorkDaySummaries)

        return roundedProjectTimes.toSortedMap()
    }
}