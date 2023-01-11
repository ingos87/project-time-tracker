package its.time.tracker

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import its.time.tracker.service.*
import its.time.tracker.service.util.*
import java.time.LocalDate
import java.time.LocalDateTime

const val MAX_WORK_HOURS_PER_DAY = 9 // TODO move this to config file

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
    val weekdaysOff by option("-w", "--weekdaysoff", help="Comma seperated list of weekdays (MON,TUE,..,SAT,SUN) when no work time is to be transferred to external systems").required()
    override fun run() {
            val service = ConfigService.createConfigService(configPath)
            service.createEmptyConfig(
                csvPath = csvPath,
                myHrSelfServiceUrl = myHrSelfServiceUrl,
                eTimeUrl = eTimeUrl,
                weekdaysOff = weekdaysOff)
    }
}

class ClockIn: CliktCommand(help="Start working on something") {
    val v: Boolean by option("-v", help = "enable verbose mode").flag()
    val topic by option("-t", "--topic", help = "time tracking topic - usually some Jira Ticket Id").required()
    val dateTimeInput by option("-d", "--datetime", help="Start datetime (format: $DATE_TIME_PATTERN) for this topic - will be NOW if left empty; today's date is prepended if only time (format: HHmm) is given")
    val configPath by option("--configpath", help = "Defines a custom config file path. That file has to be created before-hand")
    override fun run() {
        try {
            val cfg = ConfigService.createConfigService(configPath)
            val csvPath = cfg.getConfigParameterValue(ConfigService.KEY_CSV_PATH)

            val dateTime = DateTimeUtil.toValidDateTime(dateTimeInput)
            if (dateTime != null) {
                ClockEventService(v, csvPath).addClockIn(topic, dateTime as LocalDateTime)
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
            val cfg = ConfigService.createConfigService(configPath)
            val csvPath = cfg.getConfigParameterValue(ConfigService.KEY_CSV_PATH)

            val dateTime = DateTimeUtil.toValidDateTime(dateTimeInput)
            if (dateTime != null) {
                ClockEventService(v, csvPath).addClockOut(dateTime as LocalDateTime)
                echo("clock-out saved: ${DateTimeUtil.temporalToString(dateTime)}")
            }
        } catch (e: AbortException) {
            e.printMessage()
        }
    }
}

class FlexTime: CliktCommand(help="book entire days as flex time") {
    val v: Boolean by option("-v", help = "enable verbose mode").flag()
    val dateInput by option("-d", "--date", help="single date (format: $DATE_PATTERN) - will be TODAY if left empty")
    val configPath by option("--configpath", help = "Defines a custom config file path. That file has to be created before-hand")
    override fun run() {
        try {
            val cfg = ConfigService.createConfigService(configPath)
            val csvPath = cfg.getConfigParameterValue(ConfigService.KEY_CSV_PATH)

            val date = DateTimeUtil.toValidDate(dateInput)
            if (date != null) {
                ClockEventService(v, csvPath).addFlexDay(date as LocalDate)
                echo("Flex time saved for ${DateTimeUtil.temporalToString(date, DATE_PATTERN)}")
                echo("Note that now, you can no longer do clock-ins for this day.")
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
            val cfg = ConfigService.createConfigService(configPath)
            val csvPath = cfg.getConfigParameterValue(ConfigService.KEY_CSV_PATH)

            val date = DateTimeUtil.toValidDate(dateInput)
            if (date != null) {
                val service = SummaryService(v, csvPath)
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
            val cfg = ConfigService.createConfigService(configPath)
            val csvPath = cfg.getConfigParameterValue(ConfigService.KEY_CSV_PATH)

            val date = DateTimeUtil.toValidMonth(dateInput)
            if (date != null) {
                val service = SummaryService(v, csvPath)
                service.showMonthlySummary(date as LocalDate)
            }
        } catch (e: AbortException) {
            e.printMessage()
        }
    }
}

class RecordWorkingTime: CliktCommand(help="export work time for a specific calendar week to myHrSelfService") {
    val v: Boolean by option("-v", help = "enable verbose mode").flag()
    val calendarWeek by option("-w", "--weeknumber", help="date (format: $WEEK_PATTERN) - will be current week if left empty")
    val configPath by option("--configpath", help = "Defines a custom config file path. That file has to be created before-hand")
    override fun run() {
        try {
            val cfg = ConfigService.createConfigService(configPath)
            val csvPath = cfg.getConfigParameterValue(ConfigService.KEY_CSV_PATH)

            val date = DateTimeUtil.toValidCalendarWeek(calendarWeek)
            if (date != null) {
                val service = WorkingTimeService(v, csvPath)
                service.recordWorkingTime(date as LocalDate)
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
        FlexTime(),
        DailySummary(),
        MonthlySummary(),
        RecordWorkingTime())
    .main(args)
