package its.time.tracker.service

import its.time.tracker.domain.CostAssessmentPosition
import its.time.tracker.domain.WorkDaySummary
import its.time.tracker.domain.WorkDaySummaryCollection
import its.time.tracker.service.ConsoleTableHelper.Companion.getCellString
import its.time.tracker.service.ConsoleTableHelper.Companion.getContentLine
import its.time.tracker.service.ConsoleTableHelper.Companion.getHorizontalSeparator
import its.time.tracker.upload.*
import its.time.tracker.util.ClockEventsFilter
import its.time.tracker.util.DateTimeUtil
import java.time.Duration
import java.time.LocalDate
import java.util.*

class CostAssessmentService {

    fun getNormalizedCostAssessmentsForDays(uniqueDays: SortedSet<LocalDate>, forecast: Boolean): SortedMap<LocalDate, List<CostAssessmentPosition>> {
        val csvService = CsvService()
        val clockEvents = csvService.loadClockEvents()

        val summaryData = WorkDaySummaryCollection()
        uniqueDays.forEach { day ->
            val daysEvents = ClockEventsFilter.getEventsBelongingToSameDay(clockEvents, day)
            val workDaySummary = WorkDaySummary.toWorkDaySummary(daysEvents)
            val bookingPositionsList = ProjectTimeCalculator().calculateProjectTime(daysEvents)
            if (workDaySummary != null) {
                summaryData.addDay(day, workDaySummary, bookingPositionsList)
            }
        }
        if (!forecast && summaryData.data.isEmpty()) {
            println("[NO SUMMARY for ${uniqueDays.first()} - ${uniqueDays.last()} because there are no clock-in events]")
            return emptyMap<LocalDate, List<CostAssessmentPosition>>().toSortedMap()
        }

        val costAssessmentMap = summaryData.data.map { (k, v) -> k to v.second }.toMap()
        val compliantWorkDaySummaries = CostAssessmentValidator().moveProjectTimesToValidDays(costAssessmentMap)
        val roundedProjectTimes = CostAssessmentRoundingService().roundProjectTimes(compliantWorkDaySummaries)
        val withAddedAbsentDays = CostAssessmentAbsenceService().addAbsenceProjects(roundedProjectTimes, uniqueDays)

        if (forecast) {
            return CostAssessmentForecastService().applyForecast(uniqueDays, withAddedAbsentDays)
        }
        return withAddedAbsentDays
    }

    fun showCostAssessments(uniqueDays: SortedSet<LocalDate>,
                            normalizedWorkingTimes: SortedMap<LocalDate, List<CostAssessmentPosition>>) {
        if (normalizedWorkingTimes.isEmpty()) {
            return
        }

        val firstColWidth = 18
        println("[SUMMARY for ${uniqueDays.first()} - ${uniqueDays.last()}]")

        println(getHorizontalSeparator(uniqueDays, SeparatorPosition.TOP, firstColWidth, false))

        println(getContentLine(
            getCellString("weekday", firstColWidth, TextOrientation.LEFT),
            uniqueDays.map { it.dayOfWeek.name.substring(0, 3) },
            uniqueDays))
        println(getContentLine(
            getCellString("day of month", firstColWidth, TextOrientation.LEFT),
            uniqueDays.map { it.dayOfMonth.toString() },
            uniqueDays))

        val projectNames = normalizedWorkingTimes.values.flatten().map { it.project }.toSet()
        projectNames.forEach { projectName ->
            println(getHorizontalSeparator(uniqueDays, SeparatorPosition.MIDDLE, firstColWidth, false))

            val workingTimesByName = mutableMapOf<LocalDate, List<CostAssessmentPosition>>()
            normalizedWorkingTimes.forEach { (key, value) ->
                workingTimesByName[key] = value.filter { it.project == projectName }.toList()
            }

            println(getContentLine(
                getCellString(projectName, firstColWidth, TextOrientation.LEFT),
                getBookingTimesForProject(workingTimesByName, uniqueDays),
                uniqueDays))

            val topicNames = workingTimesByName.values.flatten().map { it.topic }.filter { it != "" }.toSet()
            topicNames.forEach { topicName ->
                val workingTimesByTopic = mutableMapOf<LocalDate, List<CostAssessmentPosition>>()
                workingTimesByName.forEach { (key, value) ->
                    workingTimesByTopic[key] = value.filter { it.topic == topicName }.toList()
                }

                println(getContentLine(
                    getCellString("  $topicName", firstColWidth, TextOrientation.LEFT),
                    getBookingTimesForProject(workingTimesByTopic, uniqueDays),
                    uniqueDays))

                val storyNames = workingTimesByTopic.values.flatten().map { it.story }.filter { it != "" }.toSet()
                storyNames.forEach { storyName ->
                    val workingTimesByStory = mutableMapOf<LocalDate, List<CostAssessmentPosition>>()
                    workingTimesByTopic.forEach { (key, value) ->
                        workingTimesByStory[key] = value.filter { it.story == storyName }.toList()
                    }

                    println(getContentLine(
                        getCellString("    $storyName", firstColWidth, TextOrientation.LEFT),
                        getBookingTimesForProject(workingTimesByStory, uniqueDays),
                        uniqueDays))

                }
            }
        }

        println(getHorizontalSeparator(uniqueDays, SeparatorPosition.BOTTOM, firstColWidth, false))
    }

    private fun getBookingTimesForProject(
        normalizedWorkingTimes: Map<LocalDate, List<CostAssessmentPosition>>,
        uniqueDays: SortedSet<LocalDate>
    ): List<String> {
        val times = mutableListOf<String>()
        uniqueDays.forEach { date ->
            val totalDuration = normalizedWorkingTimes[date]?.fold(Duration.ZERO) { total, position ->
                total.plus(position.totalWorkingTime)
            }
            times.add(if (totalDuration == null || totalDuration == Duration.ZERO) "     "
                        else DateTimeUtil.durationToDecimal(totalDuration).padStart(5, ' '))
        }

        return times
    }
}