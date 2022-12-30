package its.time.tracker

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

private const val TEST_CSV_PATH = "/Users/tollpatsch/test_its_times.csv"

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

fun executeClockInWitArgs(args: Array<String>) {
    main(arrayOf<String>("clock-in", "--csvpath=$TEST_CSV_PATH").plus(args))
}

fun executeClockOutWitArgs(args: Array<String>) {
    main(arrayOf<String>("clock-out", "--csvpath=$TEST_CSV_PATH").plus(args))
}

fun executeDailySummaryWitArgs(args: Array<String>) {
    main(arrayOf<String>("daily-summary", "--csvpath=$TEST_CSV_PATH").plus(args))
}

fun executeMonthlySummaryWitArgs(args: Array<String>) {
    main(arrayOf<String>("monthly-summary", "--csvpath=$TEST_CSV_PATH").plus(args))
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