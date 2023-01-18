package its.time.tracker

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import its.time.tracker.service.*
import its.time.tracker.service.util.*
import java.time.LocalDate
import java.time.LocalDateTime

class TimeTracker: CliktCommand() {
    override fun run() = Unit
}

class Version: CliktCommand(help="Show version") {
    override fun run() {
        echo("ITS TimeTracker App:: 0.0.1")
    }
}

class Init: CliktCommand(help="initializes App by writing custom properties to a file") {
    val configPath by option("-i", "--configpath", help = "Path to persistent config file").required()
    val csvPath by option("-c", "--csvpath", help = "Path to persistent file with clockins and clockouts. You should backup this file regularly").required()
    val myHrSelfServiceUrl by option("-m", "--myselfhr", help="Url to MyHRSelfService landing page").required()
    val eTimeUrl by option("-e", "--etime", help="Url to project booking landing page").required()
    val maxWorkDuration by option("-h", "--maxworkdurationperday", help="Maximum work duration per day, to which app is to fill up working time in case no explicit clock-out was submitted (e.g.: PT9H, PT8H30M)").default("PT7H42M")
    val weekdaysOff by option("-w", "--weekdaysoff", help="Comma seperated list of weekdays (MONDAY,TUESDAY,..,SATURDAY,SUNDAY) when no work time is to be transferred to external systems").default("SATURDAY,SUNDAY")
    val daysOff by option("-d", "--daysoff", help="Comma seperated list of days (format: $DATE_PATTERN) when no work time is to be transferred to external systems")
    override fun run() {
        ConfigService.createConfigFileWithParams(
            configFilePath = configPath,
            csvPath = csvPath,
            myHrSelfServiceUrl = myHrSelfServiceUrl,
            eTimeUrl = eTimeUrl,
            maxDailyWorkDuration = maxWorkDuration,
            weekdaysOff = weekdaysOff?:"",
            daysOff = daysOff?:"",
        )
    }
}

class ClockIn: CliktCommand(help="Start working on something") {
    val v: Boolean by option("-v", help = "enable verbose mode").flag()
    val topic by option("-t", "--topic", help = "time tracking topic - usually some Jira Ticket Id").required()
    val dateTimeInput by option("-d", "--datetime", help="Start datetime (format: $DATE_TIME_PATTERN) for this topic - will be NOW if left empty; today's date is prepended if only time (format: HHmm) is given")
    val configPath by option("--configpath", help = "Defines a custom config file path. That file has to be created before-hand")
    override fun run() {
        try {
            ConfigService.createConfigService(configPath).initConstants(v)

            val dateTime = DateTimeUtil.toValidDateTime(dateTimeInput)
            if (dateTime != null) {
                ClockEventService().addClockIn(topic, dateTime as LocalDateTime)
                echo("clock-in for topic '$topic' saved: ${DateTimeUtil.temporalToString(dateTime)}")
            }
        } catch (e: AbortException) {
            e.printMessage()
        }
    }
}

class ClockOut: CliktCommand(help="Interrupt or end work day") {
    val v: Boolean by option("-v", help = "enable verbose mode").flag()
    val dateTimeInput by option("-d", "--datetime", help="start datetime (format: $DATE_TIME_PATTERN) for this topic - will be NOW if left empty; today's date is prepended if only time (format: HHmm) is given")
    val configPath by option("--configpath", help = "Defines a custom config file path. That file has to be created before-hand")
    override fun run() {
        try {
            ConfigService.createConfigService(configPath).initConstants(v)

            val dateTime = DateTimeUtil.toValidDateTime(dateTimeInput)
            if (dateTime != null) {
                ClockEventService().addClockOut(dateTime as LocalDateTime)
                echo("clock-out saved: ${DateTimeUtil.temporalToString(dateTime)}")
            }
        } catch (e: AbortException) {
            e.printMessage()
        }
    }
}

class DailySummary: CliktCommand(help="show work time an project summary of a specific day") {
    val v: Boolean by option("-v", help = "enable verbose mode").flag()
    val dateInput by option("-d", "--date", help="date (format: $DATE_PATTERN) - will be today's date if left empty")
    val configPath by option("--configpath", help = "Defines a custom config file path. That file has to be created before-hand")
    override fun run() {
        try {
            ConfigService.createConfigService(configPath).initConstants(v)

            val date = DateTimeUtil.toValidDate(dateInput)
            if (date != null) {
                val service = SummaryService()
                service.showDailySummary(date as LocalDate)
            }
        } catch (e: AbortException) {
            e.printMessage()
        }
    }
}

class MonthlySummary: CliktCommand(help="show work time an project summary of a specific month") {
    val v: Boolean by option("-v", help = "enable verbose mode").flag()
    val dateInput by option("-m", "--month", help="date (format: $MONTH_PATTERN) - will be current month if left empty")
    val configPath by option("--configpath", help = "Defines a custom config file path. That file has to be created before-hand")
    override fun run() {
        try {
            ConfigService.createConfigService(configPath).initConstants(v)

            val date = DateTimeUtil.toValidMonth(dateInput)
            if (date != null) {
                val service = SummaryService()
                service.showMonthlySummary(date as LocalDate)
            }
        } catch (e: AbortException) {
            e.printMessage()
        }
    }
}

class Timekeeping: CliktCommand(help="export work time for a specific calendar week to myHrSelfService") {
    val v: Boolean by option("-v", help = "enable verbose mode").flag()
    val noop: Boolean by option("--noop", help = "Do not actually upload anything. Just show stats").flag()
    val calendarWeek by option("-w", "--weeknumber", help="date (format: $WEEK_PATTERN)")
    val month by option("-m", "--month", help="date (format: $MONTH_PATTERN)")
    val configPath by option("--configpath", help = "Defines a custom config file path. That file has to be created before-hand")
    override fun run() {
        try {
            if (calendarWeek == null && month == null || calendarWeek != null && month != null) {
                throw AbortException("Must provide --weeknumer OR --month")
            }
            ConfigService.createConfigService(configPath).initConstants(v)

            val granularity = if(month != null) Granularity.MONTH else Granularity.WEEK
            val date = when(granularity) {
                Granularity.MONTH -> DateTimeUtil.toValidMonth(month)
                else -> DateTimeUtil.toValidCalendarWeek(calendarWeek)
            }

            if (date != null) {
                val service = WorkingTimeService()
                service.captureWorkingTime(date as LocalDate, granularity, noop)
            }
        } catch (e: AbortException) {
            e.printMessage()
        }
    }
}

fun main(args: Array<String>) = TimeTracker()
    .subcommands(Version(),
        Init(),
        ClockIn(),
        ClockOut(),
        DailySummary(),
        MonthlySummary(),
        Timekeeping())
    .main(args)
