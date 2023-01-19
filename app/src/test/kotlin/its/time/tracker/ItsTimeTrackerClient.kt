package its.time.tracker

import its.time.tracker.config.ConfigService
import org.junit.platform.commons.util.StringUtils
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

const val TEST_CSV_PATH = "/tmp/its-time-tracker/test_its_times.csv"
const val TEST_CONFIG_PATH = "/tmp/its-time-tracker/app.json"

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

fun ensureTestConfig(daysOff: String = "") {
    ensureNoConfig()

    // write config
    ConfigService.createConfigFileWithParams(
        TEST_CONFIG_PATH,
        csvPath = TEST_CSV_PATH,
        myHrSelfServiceUrl = "https://no.url",
        maxDailyWorkTillAutoClockOut = "PT9H",
        eTimeUrl = "https://no.second.url",
        daysOff = daysOff,
        weekdaysOff = "SATURDAY,SUNDAY"
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
    main(arrayOf("init").plus(args))
}

fun executeClockInWitArgs(args: Array<String>) {
    main(arrayOf("clock-in", "--configpath=$TEST_CONFIG_PATH").plus(args))
}

fun executeClockOutWitArgs(args: Array<String>) {
    main(arrayOf("clock-out", "--configpath=$TEST_CONFIG_PATH").plus(args))
}

fun executeDailySummaryWitArgs(args: Array<String>) {
    main(arrayOf("daily-summary", "--configpath=$TEST_CONFIG_PATH").plus(args))
}

fun executeMonthlySummaryWitArgs(args: Array<String>) {
    main(arrayOf("monthly-summary", "--configpath=$TEST_CONFIG_PATH").plus(args))
}

fun executeUploadWorkingTimeWitArgs(args: Array<String>) {
    main(arrayOf("timekeeping", "--configpath=$TEST_CONFIG_PATH").plus(args))
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