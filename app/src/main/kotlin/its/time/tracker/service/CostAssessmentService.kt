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
    
    companion object {
        const val FIRST_COL_WIDTH = 32
    }

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

        println("[SUMMARY for ${uniqueDays.first()} - ${uniqueDays.last()}]")
        val filteredUniqueDays = uniqueDays.filter { it.dayOfWeek.ordinal < 5 }.toSortedSet() // exclude weekend days

        println(getHorizontalSeparator(filteredUniqueDays, SeparatorPosition.TOP, FIRST_COL_WIDTH, false))

        println(getContentLine(
            TableLineContent(
                getCellString("weekday", FIRST_COL_WIDTH, TextOrientation.LEFT),
                filteredUniqueDays.map { it.dayOfWeek.name.substring(0, 3) },
                null),
            filteredUniqueDays,
            0
        ))
        println(getContentLine(
            TableLineContent(
                getCellString("day of month", FIRST_COL_WIDTH, TextOrientation.LEFT),
                filteredUniqueDays.map { it.dayOfMonth.toString() },
                null),
            filteredUniqueDays,
            0
        ))

        val projectNames = normalizedWorkingTimes.values.flatten().map { it.project }.toSet()
        projectNames.forEach { projectName ->
            println(getHorizontalSeparator(filteredUniqueDays, SeparatorPosition.MIDDLE, FIRST_COL_WIDTH, false))

            val workingTimesByName = mutableMapOf<LocalDate, List<CostAssessmentPosition>>()
            normalizedWorkingTimes.forEach { (key, value) ->
                workingTimesByName[key] = value.filter { it.project == projectName }.toList()
            }

            println(getContentLine(
                getTableLineContent(projectName, workingTimesByName, filteredUniqueDays), filteredUniqueDays, 0
            ))

            val topicNames = workingTimesByName.values.flatten().map { it.topic }.filter { it != "" }.toSet()
            topicNames.forEach { topicName ->
                val workingTimesByTopic = mutableMapOf<LocalDate, List<CostAssessmentPosition>>()
                workingTimesByName.forEach { (key, value) ->
                    workingTimesByTopic[key] = value.filter { it.topic == topicName }.toList()
                }

                println(getContentLine(
                    getTableLineContent("  $topicName", workingTimesByTopic, filteredUniqueDays), filteredUniqueDays, 1
                ))

                val storyNames = workingTimesByTopic.values.flatten().map { it.story }.filter { it != "" }.toSet()
                storyNames.forEach { storyName ->
                    val workingTimesByStory = mutableMapOf<LocalDate, List<CostAssessmentPosition>>()
                    workingTimesByTopic.forEach { (key, value) ->
                        workingTimesByStory[key] = value.filter { it.story == storyName }.toList()
                    }

                    println(getContentLine(
                        getTableLineContent("    $storyName", workingTimesByStory, filteredUniqueDays),
                        filteredUniqueDays,
                        2
                    ))
                }
            }
        }

        println(getHorizontalSeparator(filteredUniqueDays, SeparatorPosition.BOTTOM, FIRST_COL_WIDTH, false))

        println(getContentLine(
            getTableLineContent("total", normalizedWorkingTimes, filteredUniqueDays), filteredUniqueDays, 0
        ))

    }

    private fun getTableLineContent(
        title: String,
        normalizedWorkingTimes: Map<LocalDate, List<CostAssessmentPosition>>,
        uniqueDays: SortedSet<LocalDate>
    ): TableLineContent {
        val times = mutableListOf<String>()
        uniqueDays.forEach { date ->
            val totalDuration = normalizedWorkingTimes[date]?.fold(Duration.ZERO) { total, position ->
                total.plus(position.totalWorkingTime)
            }
            times.add(durationToDecimal(totalDuration))
        }

        val overallDuration = normalizedWorkingTimes.values.flatten().fold(Duration.ZERO) {total, position ->
            total.plus(position.totalWorkingTime)
        }

        return TableLineContent(
            getCellString(title, FIRST_COL_WIDTH, TextOrientation.LEFT),
            times,
            durationToDecimal(overallDuration)
        )
    }

    private fun durationToDecimal(duration: Duration?) = if (duration == null || duration == Duration.ZERO) "     "
        else DateTimeUtil.durationToDecimal(duration).padStart(5, ' ')
}