package its.time.tracker

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required

const val PATTERN_FORMAT = "yyyyMMdd_HHmm"

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
    val dateTimeInput by option("-d", "--datetime", help="start datetime (format: $PATTERN_FORMAT) for this topic - will be NOW if left empty; today's date is prepended if only time (format: HHmm) is given")
    val csvPath by option("--csvpath", help = "defines path to persistent file").default(CSV_PATH)
    override fun run() {
        val dateTime = DateTimeUtil.toValidDateTime(dateTimeInput)
        if (dateTime != null) {
            echo("clock-in for topic '$topic' saved: $dateTime")
        }
    }
}

class ClockOut: CliktCommand(help="End work day") {
    val v: Boolean by option("-v", help = "enable verbose mode").flag()
    val dateTimeInput by option("-d", "--datetime", help="start datetime (format: $PATTERN_FORMAT) for this topic - will be NOW if left empty; today's date is prepended if only time (format: HHmm) is given")
    val csvPath by option("--csvpath", help = "defines path to persistent file").default(CSV_PATH)
    override fun run() {
        val dateTime = DateTimeUtil.toValidDateTime(dateTimeInput)
        if (dateTime != null) {
            StartTimeService(v, csvPath).addClockOut(dateTime)
            echo("clock-out saved: $dateTime")
        }
    }
}

fun main(args: Array<String>) = TimeTracker()
    .subcommands(Version(), ClockIn(), ClockOut())
    .main(args)
