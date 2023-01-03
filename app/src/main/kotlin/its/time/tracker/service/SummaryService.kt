package its.time.tracker.service

import its.time.tracker.DATE_PATTERN
import its.time.tracker.service.util.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.*
import java.time.temporal.ChronoUnit.DAYS

private const val CELL_WIDTH = 6

class SummaryService(
    private val verbose: Boolean,
    private val csvPath: String,
) {
    fun showDailySummary(date: LocalDate) {
        val csvService = CsvService(verbose, csvPath)
        val clockEvents = csvService.loadClockEvents()

        val daysEvents = clockEvents.filter { DateTimeUtil.isSameDay(it.dateTime, date) }.toList()
        if (daysEvents.find { it.eventType == EventType.CLOCK_IN } == null) {
            println("[NO SUMMARY for $date because there are no clock-in events]")
            return
        }

        val showWorkInProgress = DAYS.between(date, LocalDate.now()) == 0L && daysEvents.last().eventType != EventType.CLOCK_OUT

        val workTimeResult = WorkTimeCalculator().calculateWorkTime(daysEvents, showWorkInProgress)
        val bookingPositionsList = ProjectTimeCalculator().calculateProjectTime(daysEvents, showWorkInProgress)

        val cellWidth = 48
        val bookingPosLength = BookingPositionResolver.getMaxBookingPosNameLength()
        if (showWorkInProgress) {
            println("[today's work in progress]")
            println("┌" + "─".repeat(cellWidth) + "┐")
            println("│ " + "clock-in:".padEnd(20) + workTimeResult.firstClockIn.padEnd(cellWidth-21) + "│")
            println("├" + "─".repeat(cellWidth) + "┤")
            println("│ " + "current work time:".padEnd(20) + workTimeResult.totalWorkTime.padEnd(cellWidth-21) + "│")
            println("│ " + "current break time:".padEnd(20) + workTimeResult.totalBreakTime.padEnd(cellWidth-21) + "│")
        }
        else {
            println("[SUMMARY for $date]")
            println("┌" + "─".repeat(cellWidth) + "┐")
            println("│ " + "clock-in:".padEnd(18) + workTimeResult.firstClockIn.padEnd(cellWidth-19) + "│")
            println("│ " + "clock-out:".padEnd(18) + workTimeResult.lastClockOut.padEnd(cellWidth-19) + "│")
            println("├" + "─".repeat(cellWidth) + "┤")
            println("│ " + "total work time:".padEnd(18) + workTimeResult.totalWorkTime.padEnd(cellWidth-19) + "│")
            println("│ " + "total break time:".padEnd(18) + workTimeResult.totalBreakTime.padEnd(cellWidth-19) + "│")
        }

        println("├" + "═".repeat(cellWidth) + "┤")
        bookingPositionsList.forEach {
            // total width - white space - bookingPosLength - ": " - time - "  " - 1parenthesis
            val availableSpaceForTopicList = cellWidth-1-bookingPosLength-2-5-2-1
            val topicList = ("(${it.topics.joinToString(",")}".take(availableSpaceForTopicList)+")").padEnd(availableSpaceForTopicList+1)
            println("│ " + "${it.bookingKey}:".padEnd(bookingPosLength+2) + DateTimeUtil.durationToString(it.totalWorkTime) + "  " + topicList + "│")
        }
        println("└" + "─".repeat(cellWidth) + "┘")

    }

    fun showMonthlySummary(date: LocalDate) {
        val dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN, Locale.getDefault())
            .withResolverStyle(ResolverStyle.STRICT)
        val yearMonthString = dateFormatter.format(date).substring(0, 7)

        val csvService = CsvService(verbose, csvPath)
        val clockEvents = csvService.loadClockEvents()

        val monthsEvents = clockEvents.filter { DateTimeUtil.isSameMonth(it.dateTime, date) }.toList()
        if (monthsEvents.find { it.eventType == EventType.CLOCK_IN } == null) {
            println("[NO SUMMARY for $yearMonthString because there are no clock-in events]")
            return
        }

        val uniqueDays: List<LocalDate> = monthsEvents.map { DateTimeUtil.toValidDate(dateFormatter.format(it.dateTime)) as LocalDate }.toList().distinct()

        val summaryData = MonthlySummary()
        uniqueDays.forEach { day ->
            val daysEvents = clockEvents.filter { DateTimeUtil.isSameDay(it.dateTime, day) }.toList()
            val workTimeResult = WorkTimeCalculator().calculateWorkTime(daysEvents)
            val bookingPositionsList = ProjectTimeCalculator().calculateProjectTime(daysEvents)
            summaryData.addDay(day, workTimeResult, bookingPositionsList)
        }

        val firstColWidth = BookingPositionResolver.getMaxBookingPosNameLength()+2

        println("[SUMMARY for $yearMonthString]")

        println(getHorizontalSeparator(uniqueDays, SeparatorPosition.TOP, firstColWidth, false))
        println(getContentLine(
            getCellString("day of month", firstColWidth, TextOrientation.LEFT),
            uniqueDays.map { it.dayOfMonth.toString() },
            uniqueDays))
        println(getContentLine(
            getCellString("weekday", firstColWidth, TextOrientation.LEFT),
            uniqueDays.map { it.dayOfWeek.name.substring(0, 3) },
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
            summaryData.getAllTotalWorkTimes(),
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