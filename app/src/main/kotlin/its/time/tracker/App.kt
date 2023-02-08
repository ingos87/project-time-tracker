package its.time.tracker

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import its.time.tracker.config.ConfigService
import its.time.tracker.domain.CostAssessmentSetup
import its.time.tracker.exception.AbortException
import its.time.tracker.service.ClockEventService
import its.time.tracker.service.SummaryService
import its.time.tracker.service.CostAssessmentService
import its.time.tracker.service.WorkingTimeService
import its.time.tracker.util.*
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
    val myHrSelfServiceLanguage by option("-l", "--myselfhrlang", help="Language (EN,DE) of MyHRSelfService").default("EN")
    val eTimeUrl by option("-e", "--etime", help="Url to project booking landing page").required()
    val maxWorkDuration by option("-h", "--maxworkdurationperday", help="Maximum work duration per day, to which app is to fill up working time in case no explicit clock-out was submitted (e.g.: PT9H, PT8H30M)").default("PT7H42M")
    val stdWorkDuration by option("-s", "--stdworkdurationperday", help="Standard work duration per day (e.g.: PT7H42M, PT8H)").default("PT7H42M")
    val weekdaysOff by option("-w", "--weekdaysoff", help="Comma seperated list of weekdays (MONDAY,TUESDAY,..,SATURDAY,SUNDAY) when no work time is to be transferred to external systems").default("SATURDAY,SUNDAY")
    val daysOff by option("-d", "--daysoff", help="Comma seperated list of days (format: $DATE_PATTERN) when no work time is to be transferred to external systems")
    val chromeProfilePath by option("-b", "--browserprofilepath", help="Path to Chrome browser profile path (open chrome://version/)")
    override fun run() {
        ConfigService.createConfigFileWithParams(
            configFilePath = configPath,
            csvPath = csvPath,
            myHrSelfServiceUrl = myHrSelfServiceUrl,
            myHrSelfServiceLanguage = myHrSelfServiceLanguage,
            eTimeUrl = eTimeUrl,
            maxDailyWorkTillAutoClockOut = maxWorkDuration,
            weekdaysOff = weekdaysOff,
            daysOff = daysOff?:"",
            chromeProfilePath = chromeProfilePath?:"",
            standardDailyWorkDuration = stdWorkDuration,
            costAssessmentSetup = CostAssessmentSetup.getEmptyInstance()
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

class Timekeeping: CliktCommand(help="export work time for the previous 30 days or a specific calendar week to myHrSelfService") {
    val v: Boolean by option("-v", help = "enable verbose mode").flag()
    val noop: Boolean by option("--noop", help = "Do not actually upload anything. Just show compliant clock-ins and clock-outs.").flag()
    val calendarWeek by option("-w", "--week", help="date (format: $WEEK_PATTERN)")
    val configPath by option("--configpath", help = "Defines a custom config file path. That file has to be created before-hand.")
    override fun run() {
        try {
            val date: LocalDate? = DateTimeUtil.toValidCalendarWeek(calendarWeek) as LocalDate?
            ConfigService.createConfigService(configPath).initConstants(v)
            val service = WorkingTimeService()
            service.captureWorkingTime(date, noop)
        } catch (e: AbortException) {
            e.printMessage()
        }
    }
}

class CostAssessment: CliktCommand(help="export project working times for a specific calendar week to eTime") {
    val v: Boolean by option("-v", help = "enable verbose mode").flag()
    val noop: Boolean by option("--noop", help = "Do not actually upload anything. Just show working times per project per day.").flag()
    val sign: Boolean by option("-s", "--sign", help = "If set, cost assessments are automatically signed. WARNING: This cannot be undone!").flag()
    val forecast: Boolean by option("-f", "--forecast", help = "Calculate and submit cost assessment forecast for final days of billing period. This usually applies to the last 2-3 days of the last full week of every month.").flag()
    val calendarWeek by option("-w", "--week", help="date (format: $WEEK_PATTERN)").required()
    val configPath by option("--configpath", help = "Defines a custom config file path. That file has to be created before-hand")
    override fun run() {
        if (noop && sign) {
            echo("Signing cost assessments is not possible in noop mode.")
            return
        }
        try {
            ConfigService.createConfigService(configPath).initConstants(v)

            val date = DateTimeUtil.toValidCalendarWeek(calendarWeek)
            if (date != null) {
                val service = CostAssessmentService()
                service.captureProjectTimes(date as LocalDate, forecast, noop, sign)
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
        Timekeeping(),
        CostAssessment())
    .main(args)
