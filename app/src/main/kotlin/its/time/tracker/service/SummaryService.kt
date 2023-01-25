package its.time.tracker.service

import its.time.tracker.domain.EventType
import its.time.tracker.domain.WorkDaySummaryCollection
import its.time.tracker.domain.WorkDaySummary
import its.time.tracker.service.ConsoleTableHelper.Companion.getCellString
import its.time.tracker.service.ConsoleTableHelper.Companion.getContentLine
import its.time.tracker.service.ConsoleTableHelper.Companion.getHorizontalSeparator
import its.time.tracker.upload.BookingPositionResolver
import its.time.tracker.upload.ProjectTimeCalculator
import its.time.tracker.util.ClockEventsFilter
import its.time.tracker.util.DATE_PATTERN
import its.time.tracker.util.DateTimeUtil
import its.time.tracker.util.DateTimeUtil.Companion.durationToString
import its.time.tracker.util.DateTimeUtil.Companion.temporalToString
import its.time.tracker.util.TIME_PATTERN
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.*


class SummaryService {
    fun showDailySummary(date: LocalDate) {
        val csvService = CsvService()
        val clockEvents = csvService.loadClockEvents()

        //val daysEvents = clockEvents.filter { DateTimeUtil.isSameDay(it.dateTime, date) }.toList()
        val daysEvents = ClockEventsFilter.getEventsBelongingToSameDay(clockEvents, date)
        if (daysEvents.find { it.eventType == EventType.CLOCK_IN } == null) {
            println("[NO SUMMARY for $date because there are no clock-in events]")
            return
        }

        val showWorkInProgress = date.isEqual(LocalDate.now()) && daysEvents.last().eventType != EventType.CLOCK_OUT

        val workDaySummary = WorkDaySummary.toWorkDaySummary(daysEvents, showWorkInProgress)!!
        val bookingPositionsList = ProjectTimeCalculator().calculateProjectTime(daysEvents, showWorkInProgress)

        val cellWidth = 48
        val bookingPosLength = BookingPositionResolver.getMaxBookingPosNameLength()
        if (showWorkInProgress) {
            println("[today's work in progress]")
            println("┌" + "─".repeat(cellWidth) + "┐")
            println("│ " + "clock-in:".padEnd(20) + temporalToString(workDaySummary.clockIn.toLocalTime(), TIME_PATTERN).padEnd(cellWidth-21) + "│")
            println("├" + "─".repeat(cellWidth) + "┤")
            println("│ " + "current work time:".padEnd(20) + durationToString(workDaySummary.workDuration).padEnd(cellWidth-21) + "│")
            println("│ " + "current break time:".padEnd(20) + durationToString(workDaySummary.breakDuration).padEnd(cellWidth-21) + "│")
            println("│ " + "current work topic:".padEnd(20) + daysEvents.last().topic.take(21).padEnd(cellWidth-21) + "│")
        }
        else {
            println("[SUMMARY for $date]")
            println("┌" + "─".repeat(cellWidth) + "┐")
            println("│ " + "clock-in:".padEnd(18) + temporalToString(workDaySummary.clockIn.toLocalTime(), TIME_PATTERN).padEnd(cellWidth-19) + "│")
            var clockOutSupplement = ""
            if (workDaySummary.clockIn.toLocalDate().isBefore(workDaySummary.clockOut.toLocalDate())) {
                clockOutSupplement = " (${temporalToString(workDaySummary.clockOut.toLocalDate(), DATE_PATTERN)})"
            }
            println("│ " + "clock-out:".padEnd(18) + (temporalToString(workDaySummary.clockOut.toLocalTime(), TIME_PATTERN) + clockOutSupplement).padEnd(cellWidth-19) + "│")
            println("├" + "─".repeat(cellWidth) + "┤")
            println("│ " + "total work time:".padEnd(18) + durationToString(workDaySummary.workDuration).padEnd(cellWidth-19) + "│")
            println("│ " + "total break time:".padEnd(18) + durationToString(workDaySummary.breakDuration).padEnd(cellWidth-19) + "│")
        }

        println("├" + "═".repeat(cellWidth) + "┤")
        bookingPositionsList.forEach {
            // total width - white space - bookingPosLength - ": " - time - "  " - 1parenthesis
            val availableSpaceForTopicList = cellWidth-1-bookingPosLength-2-5-2-1
            val topicList = ("(${it.topics.joinToString(",")}".take(availableSpaceForTopicList)+")").padEnd(availableSpaceForTopicList+1)
            println("│ " + "${it.bookingKey}:".padEnd(bookingPosLength+2) + durationToString(it.totalWorkingTime) + "  " + topicList + "│")
        }
        println("└" + "─".repeat(cellWidth) + "┘")

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
            val bookingPositionsList = ProjectTimeCalculator().calculateProjectTime(daysEvents)
            summaryData.addDay(day, workDaySummary!!, bookingPositionsList)
        }

        val firstColWidth = BookingPositionResolver.getMaxBookingPosNameLength()+2

        println("[SUMMARY for $yearMonthString]")

        println(getHorizontalSeparator(uniqueDays, SeparatorPosition.TOP, firstColWidth, false))
        println(getContentLine(
            getCellString("weekday", firstColWidth, TextOrientation.LEFT),
            uniqueDays.map { it.dayOfWeek.name.substring(0, 3) },
            uniqueDays))
        println(getContentLine(
            getCellString("day of month", firstColWidth, TextOrientation.LEFT),
            uniqueDays.map { it.dayOfMonth.toString() },
            uniqueDays))
        println(getContentLine(
            getCellString("week of year", firstColWidth, TextOrientation.LEFT),
            uniqueDays.map { "" + Integer.parseInt(DateTimeUtil.getWeekOfYearFromDate(it)) },
            uniqueDays))
        println(getHorizontalSeparator(uniqueDays, SeparatorPosition.MIDDLE, firstColWidth, false))

        println(getContentLine(
            getCellString("clock-in", firstColWidth, TextOrientation.LEFT),
            summaryData.getAllClockIns(),
            uniqueDays))
        println(getContentLine(
            getCellString("clock-out", firstColWidth, TextOrientation.LEFT),
            summaryData.getAllClockOuts(),
            uniqueDays))

        println(getHorizontalSeparator(uniqueDays, SeparatorPosition.MIDDLE, firstColWidth, true))
        val allBookingPositionNames = summaryData.getAllBookingPositionNames()
        allBookingPositionNames.forEach { name ->
            println(getContentLine(
                getCellString(name, firstColWidth, TextOrientation.LEFT),
                summaryData.getAllBookingDurationsForKeyAsString(name),
                uniqueDays))
        }

        println(getHorizontalSeparator(uniqueDays, SeparatorPosition.MIDDLE, firstColWidth, false))
        println(getContentLine(
            getCellString("total", firstColWidth, TextOrientation.LEFT),
            summaryData.getAllTotalWorkingTimes(),
            uniqueDays))

        println(getHorizontalSeparator(uniqueDays, SeparatorPosition.BOTTOM, firstColWidth, false))
    }
}