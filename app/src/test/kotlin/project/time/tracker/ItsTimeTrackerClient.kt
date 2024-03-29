package project.time.tracker

import project.time.tracker.config.ConfigService
import project.time.tracker.domain.CostAssessmentProject
import project.time.tracker.domain.CostAssessmentSetup
import org.junit.platform.commons.util.StringUtils
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

const val TEST_CSV_PATH = "/tmp/project-time-tracker/test_project_times.csv"
const val TEST_CONFIG_PATH = "/tmp/project-time-tracker/app.json"

fun ensureCsvEmpty() {
    val path = Paths.get(TEST_CSV_PATH)

    try {
        Files.deleteIfExists(path)
    } catch (e: IOException) {
        println("Deletion failed.")
        e.printStackTrace()
    }

    File(TEST_CSV_PATH).createNewFile()
}

fun ensureTestConfig(daysOff: String,
                     sickLeave: String,
                     vacation: String) {
    ensureNoConfig()

    // write config
    ConfigService.createConfigFileWithParams(
        TEST_CONFIG_PATH,
        csvPath = TEST_CSV_PATH,
        myHrSelfServiceUrl = "https://no.url",
        myHrSelfServiceLanguage = "EN",
        eTimeUrl = "https://no.second.url",
        eTimeLanguage = "EN",
        maxDailyWorkTillAutoClockOut = "PT9H",
        weekdaysOff = "SATURDAY,SUNDAY",
        daysOff = daysOff.split(","),
        sickLeave = sickLeave.split(","),
        childSickLeave = sickLeave.split(","),
        vacation = vacation.split(","),
        chromeProfilePath = "/tmp/nowhere",
        standardDailyWorkDuration = "PT8H",
        CostAssessmentSetup(
            developmentProjects = listOf(
                CostAssessmentProject("ProjectA", "projA"),
                CostAssessmentProject("ProjectB", "projB")
            ),
            maintenanceProjects = listOf(
                CostAssessmentProject("Wartung", "wartung")
            ),
            internalProjects = listOf(
                CostAssessmentProject("Meetings", "project_meet")
            ),
            absenceProjects = listOf(
                CostAssessmentProject("Other absence", "absence")
            )
        )
    )

    // set constants
    ConfigService.createConfigService(TEST_CONFIG_PATH).initConstants(false)
}

fun ensureNoConfig() {
    if (File(TEST_CONFIG_PATH).exists()) {
        File(TEST_CONFIG_PATH).delete()
    }
}

fun executeInitWitArgs(args: Array<String>) {
    project.time.tracker.main(arrayOf("init").plus(args))
}

fun executeClockInWitArgs(args: Array<String>) {
    project.time.tracker.main(arrayOf("clock-in", "--configpath=$TEST_CONFIG_PATH").plus(args))
}

fun executeClockOutWitArgs(args: Array<String>) {
    project.time.tracker.main(arrayOf("clock-out", "--configpath=$TEST_CONFIG_PATH").plus(args))
}

fun executeDailySummaryWitArgs(args: Array<String>) {
    project.time.tracker.main(arrayOf("daily-summary", "--configpath=$TEST_CONFIG_PATH").plus(args))
}

fun executeMonthlySummaryWitArgs(args: Array<String>) {
    project.time.tracker.main(arrayOf("monthly-summary", "--configpath=$TEST_CONFIG_PATH").plus(args))
}

fun executeUploadWorkingTimeWitArgs(args: Array<String>) {
    // always use noop for tests to not accidentally upload working times to actual website
    project.time.tracker.main(arrayOf("timekeeping", "--configpath=$TEST_CONFIG_PATH", "--noop").plus(args))
}

fun executeCostAssessmentWitArgs(args: Array<String>) {
    // always use noop for tests to not accidentally upload working times to actual website
    project.time.tracker.main(arrayOf("cost-assessment", "--configpath=$TEST_CONFIG_PATH", "--noop").plus(args))
}

fun getTimesCsvContent(): List<String> {
    if (!File(TEST_CSV_PATH).exists()) {
        return emptyList()
    }

    val reader = File(TEST_CSV_PATH).inputStream().bufferedReader()
    return reader.lineSequence()
        .filter { it.isNotBlank() }
        .toList()
}

fun splitIgnoreBlank(output: String): List<String> {
    return output.split("\n").filter { StringUtils.isNotBlank(it) }
}