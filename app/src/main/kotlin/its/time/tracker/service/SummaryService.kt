package its.time.tracker.service

import its.time.tracker.domain.CostAssessmentPosition
import its.time.tracker.domain.EventType
import its.time.tracker.domain.WorkDaySummaryCollection
import its.time.tracker.domain.WorkDaySummary
import its.time.tracker.service.ConsoleTableHelper.Companion.getCellString
import its.time.tracker.service.ConsoleTableHelper.Companion.getContentLine
import its.time.tracker.service.ConsoleTableHelper.Companion.getContentLine_old
import its.time.tracker.service.ConsoleTableHelper.Companion.getHorizontalSeparator
import its.time.tracker.upload.ProjectTimeCalculator
import its.time.tracker.util.ClockEventsFilter
import its.time.tracker.util.DATE_PATTERN
import its.time.tracker.util.DateTimeUtil
import its.time.tracker.util.DateTimeUtil.Companion.temporalToString
import its.time.tracker.util.TIME_PATTERN
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.*


class SummaryService {

    companion object {
        const val FIRST_COL_WIDTH = 32
    }

    fun showDailySummary(date: LocalDate) {
        val csvService = CsvService()
        val clockEvents = csvService.loadClockEvents()

        val daysEvents = ClockEventsFilter.getEventsBelongingToSameDay(clockEvents, date)
        if (daysEvents.find { it.eventType == EventType.CLOCK_IN } == null) {
            println("[NO SUMMARY for $date because there are no clock-in events]")
            return
        }

        val showWorkInProgress = date.isEqual(LocalDate.now()) && daysEvents.last().eventType != EventType.CLOCK_OUT

        val workDaySummary = WorkDaySummary.toWorkDaySummary(daysEvents, showWorkInProgress)!!
        val costAssessmentList = ProjectTimeCalculator().calculateProjectTime(daysEvents, showWorkInProgress)

        val tableWidth = 100
        if (showWorkInProgress) {
            val currentTopicTriple = daysEvents.last().project + " -> " + daysEvents.last().topic + " -> " + daysEvents.last().story
            println("[today's work in progress]")
            println("┌" + "─".repeat(tableWidth) + "┐")
            println("│ " + "clock-in:".padEnd(20) + temporalToString(workDaySummary.clockIn.toLocalTime(), TIME_PATTERN).padEnd(tableWidth-21) + "│")
            println("├" + "─".repeat(tableWidth) + "┤")
            println("│ " + "today's work time:".padEnd(20) + DateTimeUtil.durationToString(workDaySummary.workDuration)
                .padEnd(tableWidth-21) + "│")
            println("│ " + "today's break time:".padEnd(20) + DateTimeUtil.durationToString(workDaySummary.breakDuration)
                .padEnd(tableWidth-21) + "│")
            println("│ " + "current work topic:".padEnd(20) + (currentTopicTriple + " (since ${temporalToString(daysEvents.last().dateTime.toLocalTime(), TIME_PATTERN)})").padEnd(tableWidth-21) + "│")
        }
        else {
            println("[SUMMARY for $date]")
            println("┌" + "─".repeat(tableWidth) + "┐")
            println("│ " + "clock-in:".padEnd(18) + temporalToString(workDaySummary.clockIn.toLocalTime(), TIME_PATTERN).padEnd(tableWidth-19) + "│")
            var clockOutSupplement = ""
            if (workDaySummary.clockIn.toLocalDate().isBefore(workDaySummary.clockOut.toLocalDate())) {
                clockOutSupplement = " (${temporalToString(workDaySummary.clockOut.toLocalDate(), DATE_PATTERN)})"
            }
            println("│ " + "clock-out:".padEnd(18) + (temporalToString(workDaySummary.clockOut.toLocalTime(), TIME_PATTERN) + clockOutSupplement).padEnd(tableWidth-19) + "│")
            println("├" + "─".repeat(tableWidth) + "┤")
            println("│ " + "total work time:".padEnd(18) + DateTimeUtil.durationToString(workDaySummary.workDuration)
                .padEnd(tableWidth-19) + "│")
            println("│ " + "total break time:".padEnd(18) + DateTimeUtil.durationToString(workDaySummary.breakDuration)
                .padEnd(tableWidth-19) + "│")
        }

        println("├" + "═".repeat(tableWidth) + "┤")
        val bookingPosLength = tableWidth-9
        costAssessmentList.forEach {
            val topicTriple = it.project + " -> " +it.topic + " -> " +it.story
            println("│ " + "${topicTriple.take(bookingPosLength)}:".padEnd(bookingPosLength+3) + DateTimeUtil.durationToString(it.totalWorkingTime) + "│")
        }
        println("└" + "─".repeat(tableWidth) + "┘")

    }

    fun showMonthlySummary(date: LocalDate) {
        val dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN, Locale.GERMANY)
            .withResolverStyle(ResolverStyle.STRICT)
        val yearMonthString = "${date.year}-${date.month.name.take(3)}"

        val csvService = CsvService()
        val clockEvents = csvService.loadClockEvents()

        val monthsEvents = ClockEventsFilter.getEventsBelongingToMonth(clockEvents, date)
        if (monthsEvents.find { it.eventType == EventType.CLOCK_IN } == null) {
            println("[NO SUMMARY for $yearMonthString because there are no clock-in events]")
            return
        }

        val uniqueDays = monthsEvents.map { DateTimeUtil.toValidDate(dateFormatter.format(it.dateTime)) as LocalDate }.toSortedSet()

        val summaryData = WorkDaySummaryCollection()
        uniqueDays.forEach { day ->
            val daysEvents = ClockEventsFilter.getEventsBelongingToSameDay(clockEvents, day)
            val workDaySummary = WorkDaySummary.toWorkDaySummary(daysEvents)
            val costAssessmentList = ProjectTimeCalculator().calculateProjectTime(daysEvents)
            summaryData.addDay(day, workDaySummary!!, costAssessmentList)
        }

        println("[SUMMARY for $yearMonthString]")

        println(getHorizontalSeparator(uniqueDays, SeparatorPosition.TOP, FIRST_COL_WIDTH, false))
        println(getContentLine_old(
            getCellString("weekday", FIRST_COL_WIDTH, TextOrientation.LEFT),
            uniqueDays.map { it.dayOfWeek.name.take(3)},
            uniqueDays
        ))
        println(getContentLine_old(
            getCellString("day of month", FIRST_COL_WIDTH, TextOrientation.LEFT),
            uniqueDays.map { it.dayOfMonth.toString() },
            uniqueDays
        ))
        println(getContentLine_old(
            getCellString("week of year", FIRST_COL_WIDTH, TextOrientation.LEFT),
            uniqueDays.map { "" + Integer.parseInt(DateTimeUtil.getWeekOfYearFromDate(it)) },
            uniqueDays
        ))
        println(getHorizontalSeparator(uniqueDays, SeparatorPosition.MIDDLE, FIRST_COL_WIDTH, false))

        println(getContentLine_old(
            getCellString("clock-in", FIRST_COL_WIDTH, TextOrientation.LEFT),
            summaryData.getAllClockIns(),
            uniqueDays
        ))
        println(getContentLine_old(
            getCellString("clock-out", FIRST_COL_WIDTH, TextOrientation.LEFT),
            summaryData.getAllClockOuts(),
            uniqueDays
        ))

        val projectNames = summaryData.getAllBookingPositionNames()
        projectNames.forEach { projectName ->
            println(getHorizontalSeparator(uniqueDays, SeparatorPosition.MIDDLE,
                CostAssessmentService.FIRST_COL_WIDTH, false))

            val filteredByProject = summaryData.getFilteredCostAssessmentPositionsBy(projectName, null, null)
            println(
                getContentLine(
                    getTableLineContent(projectName, filteredByProject, uniqueDays),
                    uniqueDays
                )
            )

            val topicNames = filteredByProject.values.flatten().map { it.topic }.filter { it != "" }.toSet()
            topicNames.forEach { topicName ->
                val filteredByProjectTopic = summaryData.getFilteredCostAssessmentPositionsBy(projectName, topicName, null)
                println(getContentLine(
                        getTableLineContent("  $topicName", filteredByProjectTopic, uniqueDays),
                        uniqueDays
                    )
                )

                val storyNames = filteredByProjectTopic.values.flatten().map { it.story }.filter { it != "" }.toSet()
                storyNames.forEach { storyName ->
                    val filteredByProjectTopicStory = summaryData.getFilteredCostAssessmentPositionsBy(projectName, topicName, storyName)
                    println(
                        getContentLine(
                            getTableLineContent("    $storyName", filteredByProjectTopicStory, uniqueDays),
                            uniqueDays
                        )
                    )
                }
            }
        }

        println(getHorizontalSeparator(uniqueDays, SeparatorPosition.BOTTOM, FIRST_COL_WIDTH, false))

        println(getContentLine(
            getTableLineContent("total",
                summaryData.getFilteredCostAssessmentPositionsBy(null, null, null), uniqueDays),
                uniqueDays
            )
        )
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
            times.add(durationToString(totalDuration))
        }

        val overallDuration = normalizedWorkingTimes.values.flatten().fold(Duration.ZERO) { total, position ->
            total.plus(position.totalWorkingTime)
        }

        return TableLineContent(
            getCellString(title, CostAssessmentService.FIRST_COL_WIDTH, TextOrientation.LEFT),
            times,
            durationToString(overallDuration)
        )
    }

    private fun durationToString(duration: Duration?) = if (duration == null || duration == Duration.ZERO) "     "
        else DateTimeUtil.durationToString(duration).padStart(5, ' ')
}