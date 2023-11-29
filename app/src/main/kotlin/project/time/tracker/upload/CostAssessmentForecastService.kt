package project.time.tracker.upload

import project.time.tracker.config.Constants
import project.time.tracker.domain.CostAssessmentPosition
import project.time.tracker.service.CsvService
import project.time.tracker.util.ClockEventsFilter
import project.time.tracker.util.DateTimeUtil
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

        val costAssessmentsCategories = toMapOfRoundedCostAssessmentsCategories(allCostAssessments)

        val forecastedCostAssessments = getForecastedCostAssessments(normalizedCostAssessments, costAssessmentsCategories)

        val resultingMap = mutableMapOf<LocalDate, List<CostAssessmentPosition>>()
        resultingMap.putAll(normalizedCostAssessments)
        forecastRelevantDays.forEach { resultingMap[it] = forecastedCostAssessments }

        return resultingMap.toSortedMap()
    }

    private fun getForecastedCostAssessments(
        normalizedCostAssessments: SortedMap<LocalDate, List<CostAssessmentPosition>>,
        costAssessmentsCategories: Map<String, Duration>
    ): List<CostAssessmentPosition> {
        val costAssessments = mutableListOf<CostAssessmentPosition>()
        costAssessmentsCategories.forEach { (projectCategory, duration) ->
            val project = getLastWorkedOnProject(normalizedCostAssessments, projectCategory)

            costAssessments.add(
                CostAssessmentPosition(
                    totalWorkingTime = duration,
                    project = project,
                    topic = "",
                    story = ""
                )
            )
        }

        return costAssessments.toList()
    }

    private fun getLastWorkedOnProject(
        normalizedCostAssessments: SortedMap<LocalDate, List<CostAssessmentPosition>>,
        projectCategory: String
    ): String {
        normalizedCostAssessments.keys.reversed().forEach { day ->
            val costAssessments = normalizedCostAssessments[day]!!
            val costAssessment = costAssessments
                .filter { Constants.COST_ASSESSMENT_SETUP.getProjectCategory(it.project) == projectCategory }
                .maxByOrNull { it.totalWorkingTime.toMinutes() }
            if (costAssessment != null) {
                return costAssessment.project
            }
        }

        return Constants.COST_ASSESSMENT_SETUP.getDefaultProjectFor(projectCategory)
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

    private fun toMapOfRoundedCostAssessmentsCategories(allCostAssessments: List<CostAssessmentPosition>): Map<String, Duration> {
        val costAssessmentsCategories = mutableMapOf<String, Duration>()
        var totalDuration = Duration.ZERO

        allCostAssessments.forEach {
            val projectCategory = Constants.COST_ASSESSMENT_SETUP.getProjectCategory(it.project)
            val value = costAssessmentsCategories.getOrDefault(projectCategory, Duration.ZERO)
            totalDuration += it.totalWorkingTime

            costAssessmentsCategories[projectCategory] = value.plus(it.totalWorkingTime)
        }

        if (totalDuration == Duration.ZERO) {
            return emptyMap()
        }

        val avgDevMinutes = (costAssessmentsCategories.getOrDefault(Constants.COST_ASSMNT_DEV_KEY, Duration.ZERO).toMinutes().toDouble() / totalDuration.toMinutes().toDouble() * Constants.STANDARD_WORK_DURATION_PER_DAY.toMinutes().toDouble()).toLong()
        val avgMaintMinutes = (costAssessmentsCategories.getOrDefault(Constants.COST_ASSMNT_MAINT_KEY, Duration.ZERO).toMinutes().toDouble() / totalDuration.toMinutes().toDouble() * Constants.STANDARD_WORK_DURATION_PER_DAY.toMinutes().toDouble()).toLong()
        val avgIntMinutes = (costAssessmentsCategories.getOrDefault(Constants.COST_ASSMNT_INT_KEY, Duration.ZERO).toMinutes().toDouble() / totalDuration.toMinutes().toDouble() * Constants.STANDARD_WORK_DURATION_PER_DAY.toMinutes().toDouble()).toLong()

        return mapOf(
            Constants.COST_ASSMNT_DEV_KEY to Duration.ofMinutes(avgDevMinutes),
            Constants.COST_ASSMNT_MAINT_KEY to Duration.ofMinutes(avgMaintMinutes),
            Constants.COST_ASSMNT_INT_KEY to Duration.ofMinutes(avgIntMinutes),
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