package its.time.tracker.upload

import its.time.tracker.config.Constants
import its.time.tracker.domain.CostAssessmentPosition
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.*

class CostAssessmentAbsenceService {

    fun addAbsenceProjects(costAssessmentMap: Map<LocalDate, List<CostAssessmentPosition>>, uniqueDays: SortedSet<LocalDate>)
    : SortedMap<LocalDate, List<CostAssessmentPosition>> {

        val resultingMap = mutableMapOf<LocalDate, List<CostAssessmentPosition>>()
        resultingMap.putAll(costAssessmentMap)

        uniqueDays.forEach { date ->
            if (date.dayOfWeek != DayOfWeek.SUNDAY && date.dayOfWeek != DayOfWeek.SATURDAY) {
                if (!costAssessmentMap.keys.contains(date)) {
                    resultingMap[date] = listOf(
                        CostAssessmentPosition(
                            project = "Other absence",
                            totalWorkingTime = Constants.STANDARD_WORK_DURATION_PER_DAY,
                            topics = emptySet()
                        )
                    )
                }
            }
        }

        return resultingMap.toSortedMap()
    }
}