package project.time.tracker

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import project.time.tracker.config.ConfigService
import project.time.tracker.domain.CostAssessmentSetup
import project.time.tracker.exception.AbortException
import project.time.tracker.service.ClockEventService
import project.time.tracker.service.SummaryService
import project.time.tracker.service.CostAssessmentService
import project.time.tracker.service.WorkingTimeService
import project.time.tracker.upload.CostAssessmentUploader
import project.time.tracker.util.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TimeTracker: CliktCommand() {
    override fun run() = Unit
}

class Version: CliktCommand(help="Show version") {
    override fun run() {
        echo("ProjectTimeTracker App:: 0.0.1")
    }
}

class Init: CliktCommand(help="initializes App by writing custom properties to a file") {
    val configPath by option("-i", "--configpath", help = "Path to persistent config file").required()
    val csvPath by option("-c", "--csvpath", help = "Path to persistent file with clockins and clockouts. You should backup this file regularly or have it auto-synchronized with your Dropbox(e.g.)").required()
    val myHrSelfServiceUrl by option("-m", "--myselfhr", help="Url to MyHRSelfService landing page").required()
    val myHrSelfServiceLanguage by option("-l", "--myselfhrlang", help="Language (EN,DE) of MyHRSelfService").default("EN")
    val eTimeUrl by option("-e", "--etime", help="Url to project booking landing page").required()
    val maxWorkDuration by option("-h", "--maxworkdurationperday", help="Maximum work duration per day, to which app is to fill up working time in case no explicit clock-out was submitted (e.g.: PT9H, PT8H30M)").default("PT7H42M")
    val stdWorkDuration by option("-s", "--stdworkdurationperday", help="Standard work duration per day (e.g.: PT7H42M, PT8H)").default("PT7H42M")
    val weekdaysOff by option("-w", "--weekdaysoff", help="Comma seperated list of weekdays (MONDAY,TUESDAY,..,SATURDAY,SUNDAY) when no work time is to be transferred to external systems").default("SATURDAY,SUNDAY")
    val daysOff by option("-d", "--daysoff", help="Comma seperated list of days (format: $DATE_PATTERN) when no work time is to be transferred to external systems")
    val sickLeave by option("-sl", "--sickleave", help="Comma seperated list of day ranges (format: ${DATE_PATTERN}_$DATE_PATTERN) when no work time is to be transferred to external systems")
    val childSickLeave by option("-cl", "--childsickleave", help="Comma seperated list of day ranges (format: ${DATE_PATTERN}_$DATE_PATTERN) when no work time is to be transferred to external systems")
    val vacation by option("-v", "--vacation", help="Comma seperated list of day ranges (format: ${DATE_PATTERN}_$DATE_PATTERN) when no work time is to be transferred to external systems")
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
            daysOff = daysOff?.split(",")?: emptyList(),
            sickLeave = sickLeave?.split(",")?: emptyList(),
            childSickLeave = childSickLeave?.split(",")?: emptyList(),
            vacation = vacation?.split(",")?: emptyList(),
            chromeProfilePath = chromeProfilePath?:"",
            standardDailyWorkDuration = stdWorkDuration,
            costAssessmentSetup = CostAssessmentSetup.getEmptyInstance()
        )
    }
}

class ClockIn: CliktCommand(help="Start working on something") {
    val v: Boolean by option("-v", help = "enable verbose mode").flag()
    val project by option("-p", "--project", help = "time tracking project - see eTime UI").required()
    val topic by option("-t", "--topic", help = "OPTIONAL: time tracking topic - code/meeting/etc")
    val story by option("-s", "--story", help = "OPTIONAL: time tracking story - usually some Jira Ticket Id or meeting type")
    val dateTimeInput by option("-d", "--datetime", help="OPTIONAL: Start datetime (format: $DATE_TIME_PATTERN) for this topic - will be NOW if left empty; today's date is prepended if only time (format: $TIME_PATTERN) is given")
    val configPath by option("--configpath", help = "OPTIONAL: Defines a custom config file path. That file has to be created before-hand")
    override fun run() {
        try {
            ConfigService.createConfigService(configPath).initConstants(v)

            val dateTime = DateTimeUtil.toValidDateTime(dateTimeInput)
            if (dateTime != null) {
                ClockEventService().addClockIn(project, topic, story, dateTime as LocalDateTime)
                echo("clock-in for project '$project', topic '$topic', story '$story' saved: ${DateTimeUtil.temporalToString(dateTime)}")
            }
        } catch (e: AbortException) {
            e.printMessage()
        }
    }
}

class ClockOut: CliktCommand(help="Interrupt or end work day") {
    val v: Boolean by option("-v", help = "enable verbose mode").flag()
    val dateTimeInput by option("-d", "--datetime", help="OPTIONAL: datetime (format: $DATE_TIME_PATTERN) for clockout - will be NOW if left empty; today's date is prepended if only time (format: $TIME_PATTERN) is given")
    val configPath by option("--configpath", help = "OPTIONAL: Defines a custom config file path. That file has to be created before-hand")
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
    val dateInput by option("-d", "--date", help="OPTIONAL: date (format: $DATE_PATTERN) - will be today's date if left empty")
    val configPath by option("--configpath", help = "OPTIONAL: Defines a custom config file path. That file has to be created before-hand")
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
    val configPath by option("--configpath", help = "OPTIONAL: Defines a custom config file path. That file has to be created before-hand")
    override fun run() {
        try {
            ConfigService.createConfigService(configPath).initConstants(v)
            val service = SummaryService()
            service.showMonthlySummary(LocalDate.now().minusDays(32) as LocalDate, LocalDate.now() as LocalDate)

        } catch (e: AbortException) {
            e.printMessage()
        }
    }
}

class Timekeeping: CliktCommand(help="export work time for the previous 30 days or a specific calendar week to myHrSelfService") {
    val v: Boolean by option("-v", help = "enable verbose mode").flag()
    val noop: Boolean by option("--noop", help = "OPTIONAL: Do not actually upload anything. Just show compliant clock-ins and clock-outs.").flag()
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
    val startDate by option("-d", "--startdate", help="date (format: $DATE_PATTERN)").required()
    val endDate by option("-e", "--enddate", help="date (format: $DATE_PATTERN)").required()
    val configPath by option("--configpath", help = "Defines a custom config file path. That file has to be created before-hand")
    override fun run() {
        if (noop && sign) {
            echo("Signing cost assessments is not possible in noop mode.")
            return
        }
        try {
            ConfigService.createConfigService(configPath).initConstants(v)

            val sDate = DateTimeUtil.toValidDate(startDate) as LocalDate?
            val eDate = DateTimeUtil.toValidDate(endDate) as LocalDate?
            if (sDate != null && eDate != null && sDate <= eDate) {
                val uniqueDays: HashSet<LocalDate> = HashSet()
                uniqueDays.add(sDate)
                while (uniqueDays.max() < eDate) {
                    uniqueDays.add(uniqueDays.max().plusDays(1))
                }

                val service = CostAssessmentService()
                val normalizedWorkingTimes = service.getNormalizedCostAssessmentsForDays(uniqueDays.toSortedSet(), forecast)

                service.showCostAssessments(uniqueDays.toSortedSet(), normalizedWorkingTimes)

                if (noop) {
                    println("\nNOOP mode. Uploaded nothing")
                } else {
                    println("\nUploading clock-ins and clock-outs to eTime ...")
                    CostAssessmentUploader(normalizedWorkingTimes).submit(sign)
                }
            }
        } catch (e: AbortException) {
            e.printMessage()
        }
    }
}

fun main(args: Array<String>) = project.time.tracker.TimeTracker()
    .subcommands(
        project.time.tracker.Version(),
        project.time.tracker.Init(),
        project.time.tracker.ClockIn(),
        project.time.tracker.ClockOut(),
        project.time.tracker.DailySummary(),
        project.time.tracker.MonthlySummary(),
        project.time.tracker.Timekeeping(),
        project.time.tracker.CostAssessment()
    )
    .main(args)
