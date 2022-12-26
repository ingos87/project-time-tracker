package its.time.tracker

import its.time.tracker.App.printConfig
import its.time.tracker.App.printVersion
import its.time.tracker.App.clockInWithTopic
import its.time.tracker.App.clockOut
import kotlinx.cli.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val PATTERN_FORMAT = "yyyyMMdd_HHmm"

object App {
    const val appName = "ITS TimeTracker App"
    const val version = "0.0.1"
    fun printVersion() {
        println("${appName}:: ${version}")
    }

    fun printConfig() {
        println("some day, I will list the config params here")
    }

    fun clockInWithTopic(dateTime: String, topic: String) {
        println("clock-in for topic $topic saved: $dateTime")
    }

    fun clockOut(dateTime: String) {
        println("clock-out saved: $dateTime")
    }
}


fun main(args: Array<String>) {
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(PATTERN_FORMAT)
        .withZone(ZoneId.systemDefault())

    val auxParser = ArgParser("${App.appName}:: ${App.version}")
    val version by auxParser.option(ArgType.Boolean,
        shortName = "v",
        fullName = "version",
        description = "App's version").default(false)
    val config by auxParser.option(ArgType.Boolean,
        shortName = "c",
        fullName = "config",
        description = "Configuration parameters").default(false)
    val feierabend by auxParser.option(ArgType.Boolean,
        shortName = "f",
        fullName = "feierabend",
        description = "End current day").default(false)
    /*val clockIn by auxParser.option(ArgType.Boolean,
        shortName = "s",
        fullName = "start",
        description = "Start working on something").default(false)*/
    val dateTime by auxParser.argument(ArgType.String,
        fullName = "datetime",
        description = "start datetime (format: $PATTERN_FORMAT) for this topic - will be NOW if left empty; today's date is prepended if only time is given")
        .optional()
        .default(formatter.format(Instant.now()))


    // Add all input to parser
    auxParser.parse(args)

    if(version) printVersion()
    else if (config) printConfig()
    else if (feierabend) clockOut(dateTime)


/*
    val topic by auxParser.argument(ArgType.String,
        fullName = "topic",
        description = "time tracking topic - usually some Jira Ticket Id")
        .optional()
        .default("Eierschaukeln")
    else if (clockIn) clockInWithTopic(dateTime, topic)*/
}
