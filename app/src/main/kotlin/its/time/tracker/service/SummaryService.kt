package its.time.tracker.service

import its.time.tracker.service.util.DateTimeUtil
import its.time.tracker.service.util.ProjectTimeCalculator
import its.time.tracker.service.util.WorkTimeCalculator
import java.time.LocalDate
import java.time.LocalDateTime

class SummaryService(
    private val verbose: Boolean,
    private val csvPath: String,
) {

    fun showDailyWorkHoursSummary(date: LocalDate) {
        val csvService = CsvService(verbose, csvPath)
        val clockEvents = csvService.loadClockEvents()

        val daysEvents = clockEvents.filter { DateTimeUtil.isSameDay(it.dateTime, date) }.toList()

        val workTimeResult = WorkTimeCalculator().calculateWorkTime(daysEvents)

        println("+---------------------------------------+")
        println("| work hours summary for day $date |")
        println("| clock-in:  ${workTimeResult.firstClockIn}                      |")
        println("| clock-out: ${workTimeResult.lastClockOut}                      |")
        println("|_____________                          |")
        println("| total work time:  ${workTimeResult.totalWorkTime}               |")
        println("| total break time: ${workTimeResult.totalBreakTime}               |")
        println("+---------------------------------------+")
    }

    fun showDailyProjectSummary(date: LocalDate) {
        val csvService = CsvService(verbose, csvPath)
        val clockEvents = csvService.loadClockEvents()

        val daysEvents = clockEvents.filter { DateTimeUtil.isSameDay(it.dateTime, date) }.toList()

        val workTimeResult = ProjectTimeCalculator().calculateProjectTime(daysEvents)

        println("+---------------------------------------+")
        println("| project summary for day $date")
        workTimeResult.forEach {
            println("| ${it.bookingKey}: ${DateTimeUtil.durationToString(it.totalWorkTime)}  (${it.topics.joinToString(",")})")
        }
        println("+---------------------------------------+")
    }
}