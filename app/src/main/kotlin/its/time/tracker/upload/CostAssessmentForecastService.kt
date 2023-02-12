package its.time.tracker.upload

import its.time.tracker.config.Constants
import its.time.tracker.domain.CostAssessmentPosition
import its.time.tracker.domain.CostAssessmentSetup
import its.time.tracker.domain.WorkDaySummary
import its.time.tracker.service.CostAssessmentService.Companion.combineByBookingId
import its.time.tracker.service.CsvService
import its.time.tracker.util.ClockEventsFilter
import its.time.tracker.util.DateTimeUtil
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.util.*

class CostAssessmentForecastService {

    fun roundProjectTimes(uniqueDays: List<LocalDate>, normalizedCostAssessments: SortedMap<LocalDate, List<CostAssessmentPosition>>
    ): SortedMap<LocalDate, List<CostAssessmentPosition>> {
        val forecastRelevantDays = getForecastRelevantDays(uniqueDays, normalizedCostAssessments)

        if (forecastRelevantDays.isEmpty()) {
            return normalizedCostAssessments
        }

        val csvService = CsvService()
        val clockEvents = csvService.loadClockEvents()

        val allCostAssessments = mutableListOf<CostAssessmentPosition>()

        var currentDate = uniqueDays.first().minusDays(30)
        while (currentDate.isBefore(normalizedCostAssessments.lastKey())) {
            val daysEvents = ClockEventsFilter.getEventsBelongingToSameDay(clockEvents, currentDate)
            allCostAssessments.addAll(ProjectTimeCalculator().calculateProjectTime(daysEvents))

            currentDate = currentDate.plusDays(1)
        }

        val superiorCostAssessments = mutableMapOf<String, Duration>()
        allCostAssessments.forEach {
            val superiorProject = Constants.COST_ASSESSMENT_SETUP.getSuperiorProject(it.project)
            val value = superiorCostAssessments.getOrDefault(superiorProject, Duration.ZERO)

            superiorCostAssessments.put(superiorProject, value.plus(it.totalWorkingTime))
        }


        // todo calc percentages
        // todo get latest projects from out of superior cost assessments
        // todo create set of CostAssessmentPosition with rounded values adding up to avg. working day

        return normalizedCostAssessments // todo return map with added days (forecast cost assessments)
    }

    private fun getForecastRelevantDays(
        uniqueDays: List<LocalDate>,
        normalizedCostAssessments: SortedMap<LocalDate, List<CostAssessmentPosition>>
    ): List<LocalDate> {
        return uniqueDays.filter {
            DateTimeUtil.isWorkingDay(it)
                    && !normalizedCostAssessments.keys.contains(it)
                    && it.isAfter(normalizedCostAssessments.lastKey())
        }
    }
}