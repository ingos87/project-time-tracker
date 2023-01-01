package its.time.tracker.service

import its.time.tracker.DATE_PATTERN
import its.time.tracker.service.util.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.*

private const val CELL_WIDTH = 6

class SummaryService(
    private val verbose: Boolean,
    private val csvPath: String,
) {

    fun showDailyWorkHoursSummary(date: LocalDate) {
        val csvService = CsvService(verbose, csvPath)
        val clockEvents = csvService.loadClockEvents()

        val daysEvents = clockEvents.filter { DateTimeUtil.isSameDay(it.dateTime, date) }.toList()

        val workTimeResult = WorkTimeCalculator().calculateWorkTime(daysEvents)

        println("=== SUMMARY for $date ===")
        println("+---------------------------------------+")
        println("| clock-in:  ${workTimeResult.firstClockIn}                      |")
        println("| clock-out: ${workTimeResult.lastClockOut}                      |")
        println("|_________________                      |")
        println("| total work time:  ${workTimeResult.totalWorkTime}               |")
        println("| total break time: ${workTimeResult.totalBreakTime}               |")
    }

    fun showDailyProjectSummary(date: LocalDate) {
        val csvService = CsvService(verbose, csvPath)
        val clockEvents = csvService.loadClockEvents()

        val daysEvents = clockEvents.filter { DateTimeUtil.isSameDay(it.dateTime, date) }.toList()

        val bookingPositionsList = ProjectTimeCalculator().calculateProjectTime(daysEvents)

        println("+=======================================+")
        bookingPositionsList.forEach {
            println("| ${it.bookingKey}: ${DateTimeUtil.durationToString(it.totalWorkTime)}  (${it.topics.joinToString(",")})")
        }
        println("+---------------------------------------+")
    }

    fun showMonthlySummary(date: LocalDate) {
        val csvService = CsvService(verbose, csvPath)
        val clockEvents = csvService.loadClockEvents()

        val monthsEvents = clockEvents.filter { DateTimeUtil.isSameMonth(it.dateTime, date) }.toList()

        val dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN, Locale.getDefault())
            .withResolverStyle(ResolverStyle.STRICT)
        val uniqueDays: List<LocalDate> = monthsEvents.map { DateTimeUtil.toValidDate(dateFormatter.format(it.dateTime)) as LocalDate }.toList().distinct()

        val summaryData = MonthlySummary()
        uniqueDays.forEach { day ->
            val daysEvents = clockEvents.filter { DateTimeUtil.isSameDay(it.dateTime, day) }.toList()
            val workTimeResult = WorkTimeCalculator().calculateWorkTime(daysEvents)
            val bookingPositionsList = ProjectTimeCalculator().calculateProjectTime(daysEvents)
            summaryData.addDay(day, workTimeResult, bookingPositionsList)
        }

        val firstColWidth = BookingPositionResolver.getMaxBookingPosNameLength()+2
        val separatorLine = "+" + "-".repeat(firstColWidth) + "+" + ("-".repeat(CELL_WIDTH) + "+").repeat(uniqueDays.size)

        println("=== SUMMARAY for ${dateFormatter.format(date).substring(0, 7)} ===")

        println(separatorLine)
        println(getContentLine(
            getCellString("day of month", firstColWidth, TextOrientation.LEFT),
            uniqueDays.map { it.dayOfMonth.toString() }))
        println(getContentLine(
            getCellString("weekday", firstColWidth, TextOrientation.LEFT),
            uniqueDays.map { it.dayOfWeek.name.substring(0, 3) }))
        println(separatorLine)

        println(getContentLine(
            getCellString("clock-in", firstColWidth, TextOrientation.LEFT),
            summaryData.getAllClockIns()))
        println(getContentLine(
            getCellString("clock-out", firstColWidth, TextOrientation.LEFT),
            summaryData.getAllClockOuts()))

        println(separatorLine)
        val allBookinkingPositionNames = summaryData.getAllBookingPositionNames()
        allBookinkingPositionNames.forEach { name ->
            println(getContentLine(
                getCellString(name, firstColWidth, TextOrientation.LEFT),
                summaryData.getAllBookingDurationsForKey(name)))
        }

        println(separatorLine)
        println(getContentLine(
            getCellString("total", firstColWidth, TextOrientation.LEFT),
            summaryData.getAllTotalWorkTimes()))
    }

    private fun getContentLine(title: String, values: List<String>): String {
        var line = "|$title|"
        values.forEach {
            line += getCellString(it, CELL_WIDTH, TextOrientation.CENTER) + "|"
        }
        return line
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