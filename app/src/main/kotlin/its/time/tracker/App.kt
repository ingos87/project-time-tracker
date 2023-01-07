package its.time.tracker

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import its.time.tracker.service.ClockEventService
import its.time.tracker.service.ConfigService
import its.time.tracker.service.SummaryService
import its.time.tracker.service.util.DateTimeUtil
import java.time.LocalDate
import java.time.LocalDateTime

const val CSV_PATH = "/Users/tollpatsch/its_times.csv"

const val DATE_TIME_PATTERN = "uuuu-MM-dd HH:mm"
const val DATE_PATTERN = "uuuu-MM-dd"
const val TIME_PATTERN = "HH:mm"
const val MONTH_PATTERN = "uuuu-MM"

const val appName = "ITS TimeTracker App"
const val version = "0.0.1"

const val MAX_WORK_HOURS_PER_DAY = 9

class TimeTracker: CliktCommand() {
    override fun run() = Unit
}

class Version: CliktCommand(help="Show version") {
    override fun run() {
        echo("${appName}:: ${version}")
    }
}

class Init: CliktCommand(help="initializes App and writes custom properties") {
    val csvPath by option("--csvpath", help = "path to persistent file").required()
    val myHrSelfServiceUrl by option("-m", "--myselfhr", help="Url to MyHRSelfService landing page").required()
    val eTimeUrl by option("-e", "--etime", help="Url to project booking landing page").required()
    override fun run() {
            val service = ConfigService()
            service.createEmptyConfig(csvPath, myHrSelfServiceUrl, eTimeUrl)
    }
}

class ClockIn: CliktCommand(help="Start working on something") {
    val v: Boolean by option("-v", help = "enable verbose mode").flag()
    val topic by option("-t", "--topic", help = "time tracking topic - usually some Jira Ticket Id").required()
    val dateTimeInput by option("-d", "--datetime", help="start datetime (format: $DATE_TIME_PATTERN) for this topic - will be NOW if left empty; today's date is prepended if only time (format: HHmm) is given")
    val csvPath by option("--csvpath", help = "defines path to persistent file. default: $CSV_PATH").default(CSV_PATH)
    override fun run() {
        val dateTime = DateTimeUtil.toValidDateTime(dateTimeInput)
        if (dateTime != null) {
            val success = ClockEventService(v, csvPath).addClockIn(topic, dateTime as LocalDateTime)
            if (success) echo("clock-in for topic '$topic' saved: ${DateTimeUtil.dateTimeToString(dateTime)}")
        }
    }
}

class ClockOut: CliktCommand(help="Interrupt or end work day") {
    val v: Boolean by option("-v", help = "enable verbose mode").flag()
    val dateTimeInput by option("-d", "--datetime", help="start datetime (format: $DATE_TIME_PATTERN) for this topic - will be NOW if left empty; today's date is prepended if only time (format: HHmm) is given")
    val csvPath by option("--csvpath", help = "defines path to persistent file. default: $CSV_PATH").default(CSV_PATH)
    override fun run() {
        val dateTime = DateTimeUtil.toValidDateTime(dateTimeInput)
        if (dateTime != null) {
            val success = ClockEventService(v, csvPath).addClockOut(dateTime as LocalDateTime)
            if (success) echo("clock-out saved: ${DateTimeUtil.dateTimeToString(dateTime)}")
        }
    }
}

class DailySummary: CliktCommand(help="show work time an project summary of a specific day") {
    val v: Boolean by option("-v", help = "enable verbose mode").flag()
    val dateInput by option("-d", "--date", help="date (format: $DATE_PATTERN) - will be today's date if left empty")
    val csvPath by option("--csvpath", help = "defines path to persistent file. default: $CSV_PATH").default(CSV_PATH)
    override fun run() {
        val date = DateTimeUtil.toValidDate(dateInput)
        if (date != null) {
            val service = SummaryService(v, csvPath)
            service.showDailySummary(date as LocalDate)
        }
    }
}

class MonthlySummary: CliktCommand(help="show work time an project summary of a specific month") {
    val v: Boolean by option("-v", help = "enable verbose mode").flag()
    val dateInput by option("-m", "--month", help="date (format: $MONTH_PATTERN) - will be current month if left empty")
    val csvPath by option("--csvpath", help = "defines path to persistent file. default: $CSV_PATH").default(CSV_PATH)
    override fun run() {
        val date = DateTimeUtil.toValidMonth(dateInput)
        if (date != null) {
            val service = SummaryService(v, csvPath)
            service.showMonthlySummary(date as LocalDate)
        }
    }
}

fun main(args: Array<String>) = TimeTracker()
    .subcommands(Version(), Init(), ClockIn(), ClockOut(), DailySummary(), MonthlySummary())
    .main(args)
