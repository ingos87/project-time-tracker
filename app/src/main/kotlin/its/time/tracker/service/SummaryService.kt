package its.time.tracker.service

import its.time.tracker.domain.EventType
import its.time.tracker.domain.MonthlySummary
import its.time.tracker.domain.WorkDaySummary
import its.time.tracker.service.util.DATE_PATTERN
import its.time.tracker.service.util.*
import its.time.tracker.service.util.DateTimeUtil.Companion.durationToString
import its.time.tracker.service.util.DateTimeUtil.Companion.temporalToString
import its.time.tracker.upload.BookingPositionResolver
import its.time.tracker.upload.ProjectTimeCalculator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.*
import java.time.temporal.ChronoUnit.DAYS

private const val CELL_WIDTH = 6

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
        val yearMonthString = dateFormatter.format(date).substring(0, 7)

        val csvService = CsvService()
        val clockEvents = csvService.loadClockEvents()

        val monthsEvents = ClockEventsFilter.getEventsBelongingToMonth(clockEvents, date)
        if (monthsEvents.find { it.eventType == EventType.CLOCK_IN } == null) {
            println("[NO SUMMARY for $yearMonthString because there are no clock-in events]")
            return
        }

        val uniqueDays: List<LocalDate> = monthsEvents.map { DateTimeUtil.toValidDate(dateFormatter.format(it.dateTime)) as LocalDate }.toList().distinct()

        val summaryData = MonthlySummary()
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
                summaryData.getAllBookingDurationsForKey(name),
                uniqueDays))
        }

        println(getHorizontalSeparator(uniqueDays, SeparatorPosition.MIDDLE, firstColWidth, false))
        println(getContentLine(
            getCellString("total", firstColWidth, TextOrientation.LEFT),
            summaryData.getAllTotalWorkingTimes(),
            uniqueDays))

        println(getHorizontalSeparator(uniqueDays, SeparatorPosition.BOTTOM, firstColWidth, false))
    }

    private fun getHorizontalSeparator(
        uniqueDays: List<LocalDate>,
        separatorPosition: SeparatorPosition,
        firstColWidth: Int,
        isDoubleLine: Boolean
    ): String {

        val lineElem = if(isDoubleLine) "═" else "─"
        val frontJunction = when(separatorPosition) {
            SeparatorPosition.TOP ->    "┌"
            SeparatorPosition.MIDDLE -> "├"
            SeparatorPosition.BOTTOM -> "└"
        }

        val centerJunction = when(separatorPosition) {
            SeparatorPosition.TOP ->    "┬"
            SeparatorPosition.MIDDLE -> "┼"
            SeparatorPosition.BOTTOM -> "┴"
        }

        val doubleCenterJunction = when(separatorPosition) {
            SeparatorPosition.TOP ->    "╦"
            SeparatorPosition.MIDDLE -> "╬"
            SeparatorPosition.BOTTOM -> "╩"
        }

        val endJunction = when(separatorPosition) {
            SeparatorPosition.TOP ->    "┐"
            SeparatorPosition.MIDDLE -> "┤"
            SeparatorPosition.BOTTOM -> "┘"
        }

        val separatorLine = StringBuilder()
        separatorLine.append(frontJunction + lineElem.repeat(firstColWidth))

        val horizontalLineElem = lineElem.repeat(CELL_WIDTH)
        for (i in uniqueDays.indices) {
            if (needsDoubleLineDueToDaysDiff(uniqueDays, i)) {
                separatorLine.append(doubleCenterJunction)
            }
            else {
                separatorLine.append(centerJunction)
            }
            separatorLine.append(horizontalLineElem)
        }

        separatorLine.append(endJunction)

        return separatorLine.toString()
    }

    private fun getContentLine(title: String, values: List<String>, uniqueDays: List<LocalDate>): String {
        val lineBuilder = StringBuilder()
        lineBuilder.append("│$title")

        for (i in values.indices) {
            if (needsDoubleLineDueToDaysDiff(uniqueDays, i)) {
                lineBuilder.append("║")
            }
            else {
                lineBuilder.append("│")
            }
            lineBuilder.append(getCellString(values[i], CELL_WIDTH, TextOrientation.CENTER))
        }

        lineBuilder.append("│")

        return lineBuilder.toString()
    }

    private fun needsDoubleLineDueToDaysDiff(uniqueDays: List<LocalDate>, currentIdx: Int): Boolean {
        if (currentIdx > 0) {
            val prevDate = uniqueDays[currentIdx-1]
            val date = uniqueDays[currentIdx]
            return DAYS.between(prevDate, date) > 1
        }
        return false
    }

    private fun getCellString(content: String, cellWidth: Int, textOrientation: TextOrientation): String {
        return when (textOrientation) {
            TextOrientation.CENTER -> when (content.length) {
                0 -> " ".repeat(cellWidth)
                1 -> " ".repeat(cellWidth-2) + content + " "
                2 -> " ".repeat(cellWidth-3) + content + " "
                3 -> " ".repeat(cellWidth-4) + content + " "
                4 -> " ".repeat(cellWidth-5) + content + " "
                5 -> " ".repeat(cellWidth-5) + content
                6 -> " ".repeat(cellWidth-6) + content
                else -> content.substring(0, cellWidth)
            }
            TextOrientation.LEFT -> {
                " " + content.padEnd(cellWidth-1)
            }
            TextOrientation.RIGHT -> {
                " " + content.padStart(cellWidth-1)
            }
        }
    }
}

enum class TextOrientation {
    LEFT, RIGHT, CENTER
}

enum class SeparatorPosition {
    TOP, MIDDLE, BOTTOM
}