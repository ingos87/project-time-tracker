package its.time.tracker.upload

import its.time.tracker.config.Constants
import its.time.tracker.domain.CostAssessmentPosition
import its.time.tracker.service.CsvService
import its.time.tracker.util.ClockEventsFilter
import its.time.tracker.util.DateTimeUtil
import java.time.Duration
import java.time.LocalDate
import java.util.*
import kotlin.math.min

class CostAssessmentForecastService {

    fun applyForecast(
        uniqueDays: SortedSet<LocalDate>,
        normalizedCostAssessments: SortedMap<LocalDate, List<CostAssessmentPosition>>
    ): SortedMap<LocalDate, List<CostAssessmentPosition>> {
        val forecastRelevantDays = getForecastRelevantDays(uniqueDays, normalizedCostAssessments)

        if (forecastRelevantDays.isEmpty()) {
            return normalizedCostAssessments
        }

        val allCostAssessments = summedUpCostAssessments(
            uniqueDays.first().minusDays(30),
            normalizedCostAssessments.lastKey())

        val superiorCostAssessments = toMapOfRoundedSuperiorCostAssessments(allCostAssessments)

        val forecastedCostAssessments = getForecastedCostAssessments(normalizedCostAssessments, superiorCostAssessments)

        val resultingMap = mutableMapOf<LocalDate, List<CostAssessmentPosition>>()
        resultingMap.putAll(normalizedCostAssessments)
        forecastRelevantDays.forEach { resultingMap[it] = forecastedCostAssessments }

        return resultingMap.toSortedMap()
    }

    private fun getForecastedCostAssessments(
        normalizedCostAssessments: SortedMap<LocalDate, List<CostAssessmentPosition>>,
        superiorCostAssessments: Map<String, Duration>
    ): List<CostAssessmentPosition> {
        val costAssessments = mutableListOf<CostAssessmentPosition>()
        superiorCostAssessments.forEach { (superiorProject, duration) ->
            val project = getLastWorkedOnProject(normalizedCostAssessments, superiorProject)

            costAssessments.add(
                CostAssessmentPosition(
                    project = project,
                    totalWorkingTime = duration,
                    topics = emptySet()
                )
            )
        }

        return costAssessments.toList()
    }

    private fun getLastWorkedOnProject(
        normalizedCostAssessments: SortedMap<LocalDate, List<CostAssessmentPosition>>,
        superiorProject: String
    ): String {
        normalizedCostAssessments.keys.reversed().forEach { day ->
            val costAssessments = normalizedCostAssessments[day]!!
            val costAssessment = costAssessments
                .filter { Constants.COST_ASSESSMENT_SETUP.getSuperiorProject(it.project) == superiorProject }
                .maxByOrNull { it.totalWorkingTime.toMinutes() }
            if (costAssessment != null) {
                return costAssessment.project
            }
        }

        return Constants.COST_ASSESSMENT_SETUP.getDefaultProjectFor(superiorProject)
    }

    private fun summedUpCostAssessments(firstDay: LocalDate, lastDay: LocalDate): List<CostAssessmentPosition> {
        val csvService = CsvService()
        val clockEvents = csvService.loadClockEvents()

        val allCostAssessments = mutableListOf<CostAssessmentPosition>()

        var currentDate = firstDay
        while (currentDate.isBefore(lastDay.plusDays(1))) {
            val daysEvents = ClockEventsFilter.getEventsBelongingToSameDay(clockEvents, currentDate)
            allCostAssessments.addAll(ProjectTimeCalculator().calculateProjectTime(daysEvents))

            currentDate = currentDate.plusDays(1)
        }

        return allCostAssessments.toList()
    }

    private fun toMapOfRoundedSuperiorCostAssessments(allCostAssessments: List<CostAssessmentPosition>): Map<String, Duration> {
        val superiorCostAssessments = mutableMapOf<String, Duration>()
        var totalDuration = Duration.ZERO

        allCostAssessments.forEach {
            val superiorProject = Constants.COST_ASSESSMENT_SETUP.getSuperiorProject(it.project)
            val value = superiorCostAssessments.getOrDefault(superiorProject, Duration.ZERO)
            totalDuration += it.totalWorkingTime

            superiorCostAssessments[superiorProject] = value.plus(it.totalWorkingTime)
        }

        if (totalDuration == Duration.ZERO) {
            return emptyMap()
        }

        val avgDevMinutes = (superiorCostAssessments.getOrDefault(Constants.COST_ASSMNT_DEV_KEY, Duration.ZERO).toMinutes().toDouble() / totalDuration.toMinutes().toDouble() * Constants.STANDARD_WORK_DURATION_PER_DAY.toMinutes().toDouble()).toLong()
        val avgMaintMinutes = (superiorCostAssessments.getOrDefault(Constants.COST_ASSMNT_MAINT_KEY, Duration.ZERO).toMinutes().toDouble() / totalDuration.toMinutes().toDouble() * Constants.STANDARD_WORK_DURATION_PER_DAY.toMinutes().toDouble()).toLong()
        val avgIntMinutes = (superiorCostAssessments.getOrDefault(Constants.COST_ASSMNT_INT_KEY, Duration.ZERO).toMinutes().toDouble() / totalDuration.toMinutes().toDouble() * Constants.STANDARD_WORK_DURATION_PER_DAY.toMinutes().toDouble()).toLong()
        val avgAbscMinutes = (superiorCostAssessments.getOrDefault(Constants.COST_ASSMNT_ABSC_KEY, Duration.ZERO).toMinutes().toDouble() / totalDuration.toMinutes().toDouble() * Constants.STANDARD_WORK_DURATION_PER_DAY.toMinutes().toDouble()).toLong()

        return mapOf(
            Constants.COST_ASSMNT_DEV_KEY to Duration.ofMinutes(avgDevMinutes),
            Constants.COST_ASSMNT_MAINT_KEY to Duration.ofMinutes(avgMaintMinutes),
            Constants.COST_ASSMNT_INT_KEY to Duration.ofMinutes(avgIntMinutes),
            Constants.COST_ASSMNT_ABSC_KEY to Duration.ofMinutes(avgAbscMinutes),
        ).filter { (_, v) -> v > Duration.ZERO }
            .map { (k, v) -> k to Duration.ofMinutes(min(Constants.STANDARD_WORK_DURATION_PER_DAY.toMinutes(), v.toMinutes())) }.toMap()
            .map { (k, v) -> k to DateTimeUtil.roundToHalfHourWithRemainder(v).first }.toMap()
    }

    private fun getForecastRelevantDays(
        uniqueDays: SortedSet<LocalDate>,
        normalizedCostAssessments: SortedMap<LocalDate, List<CostAssessmentPosition>>
    ): List<LocalDate> {
        return uniqueDays.filter {
            DateTimeUtil.isWorkingDay(it)
                    && !normalizedCostAssessments.keys.contains(it)
                    && it.isAfter(normalizedCostAssessments.lastKey())
        }
    }
}