package its.time.tracker

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import its.time.tracker.service.StartTimeService
import its.time.tracker.service.SummaryService
import its.time.tracker.service.util.DateTimeUtil

const val CLOCK_EVENT_PATTERN_FORMAT = "yyyyMMdd_HHmm"
const val DAY_PATTERN_FORMAT = "yyyyMMdd"

const val appName = "ITS TimeTracker App"
const val version = "0.0.1"

const val CSV_PATH = "/Users/tollpatsch/its_times.csv"

class TimeTracker: CliktCommand() {
    override fun run() = Unit
}

class Version: CliktCommand(help="Show version") {
    override fun run() {
        echo("${appName}:: ${version}")
    }
}

class ClockIn: CliktCommand(help="Start working on something") {
    val v: Boolean by option("-v", help = "enable verbose mode").flag()
    val topic by option("-t", "--topic", help = "time tracking topic - usually some Jira Ticket Id").required()
    val dateTimeInput by option("-d", "--datetime", help="start datetime (format: $CLOCK_EVENT_PATTERN_FORMAT) for this topic - will be NOW if left empty; today's date is prepended if only time (format: HHmm) is given")
    val csvPath by option("--csvpath", help = "defines path to persistent file").default(CSV_PATH)
    override fun run() {
        val dateTime = DateTimeUtil.toValidDateTime(dateTimeInput)
        if (dateTime != null) {
            val success = StartTimeService(v, csvPath).addClockIn(topic, dateTime)
            if (success) echo("clock-in for topic '$topic' saved: $dateTime")
        }
    }
}

class ClockOut: CliktCommand(help="End work day") {
    val v: Boolean by option("-v", help = "enable verbose mode").flag()
    val dateTimeInput by option("-d", "--datetime", help="start datetime (format: $CLOCK_EVENT_PATTERN_FORMAT) for this topic - will be NOW if left empty; today's date is prepended if only time (format: HHmm) is given")
    val csvPath by option("--csvpath", help = "defines path to persistent file").default(CSV_PATH)
    override fun run() {
        val dateTime = DateTimeUtil.toValidDateTime(dateTimeInput)
        if (dateTime != null) {
            val success = StartTimeService(v, csvPath).addClockOut(dateTime)
            if (success) echo("clock-out saved: $dateTime")
        }
    }
}

class DailySummary: CliktCommand(help="show summary of specified day") {
    val v: Boolean by option("-v", help = "enable verbose mode").flag()
    val dateInput by option("-d", "--date", help="date (format: $DAY_PATTERN_FORMAT) - will be today's date if left empty")
    val csvPath by option("--csvpath", help = "defines path to persistent file").default(CSV_PATH)
    override fun run() {
        val date = DateTimeUtil.toValidDate(dateInput)
        if (date != null) {
            SummaryService(v, csvPath).showDailySummary(date)
        }
    }
}

fun main(args: Array<String>) = TimeTracker()
    .subcommands(Version(), ClockIn(), ClockOut(), DailySummary())
    .main(args)
