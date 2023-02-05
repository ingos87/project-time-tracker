package its.time.tracker.upload

import its.time.tracker.domain.CostAssessmentPosition
import its.time.tracker.domain.WorkDaySummary
import its.time.tracker.domain.WorkDaySummaryCollection
import java.time.LocalDate
import java.util.*

class CostAssessmentNormalizer {

    fun normalizeWorkingTime(costAssessmentMap: Map<LocalDate, List<CostAssessmentPosition>>): SortedMap<LocalDate, List<CostAssessmentPosition>> {
        val compliantWorkDaySummaries = CostAssessmentValidator().moveProjectTimesToValidDays(costAssessmentMap)
        val roundedProjectTimes = CostAssessmentRoundingService().roundProjectTimes(compliantWorkDaySummaries)

        return roundedProjectTimes.toSortedMap()
    }
}