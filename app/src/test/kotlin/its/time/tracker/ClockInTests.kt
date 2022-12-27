package its.time.tracker

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErrAndOut
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val TEST_CSV_PATH = "/Users/tollpatsch/its_times.csv"

class ClockInTests : FunSpec({

    beforeEach {
        val path = Paths.get(TEST_CSV_PATH)

        try {
            withContext(Dispatchers.IO) {
                Files.deleteIfExists(path)
            }
        } catch (e: IOException) {
            println("Deletion failed.")
            e.printStackTrace()
        }
    }

    test("clock-in is saved with current time") {
        val output = tapSystemOut {
            main(arrayOf<String>("clock-in", "-tEPP-007"))
        }

        output shouldStartWith "clock-in for topic 'EPP-007' saved: 20"
        output.length shouldBeExactly 50
    }

    test("clock-in is saved with manual time") {
        val output = tapSystemOut {
            main(arrayOf<String>("clock-in", "-tEPP-007", "--datetime=20221223_1730"))
        }

        output shouldBe "clock-in for topic 'EPP-007' saved: 20221223_1730\n"
        getTimesCsvContent() shouldBe emptyList() // FIXME implement
    }

    test("clock-in is saved with today's date if only time is given") {
        val output = tapSystemOut {
            main(arrayOf<String>("clock-in", "-tEPP-007", "-d0534"))
        }

        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneId.systemDefault())
        val today = formatter.format(Instant.now())

        output shouldBe "clock-in for topic 'EPP-007' saved: ${today}_0534\n"
    }

    test("clock-in is discarded if date is invalid") {
        val output = tapSystemErrAndOut {
            main(arrayOf<String>("clock-in", "-tEPP-007", "-d20200132"))
        }

        output shouldBe "invalid datetime input '20200132'\n"
    }

    test("clock-in is discarded if time is invalid") {
        val output = tapSystemErrAndOut {
            main(arrayOf<String>("clock-in", "-tEPP-007", "-d1961"))
        }

        output shouldBe "invalid datetime input '1961'\n"
    }
})

fun getTimesCsvContent(): List<String> {
    if (!File(TEST_CSV_PATH).exists()) {
        return emptyList()
    }

    val reader = File(TEST_CSV_PATH).inputStream().bufferedReader()
    reader.readLine()
    return reader.lineSequence()
        .filter { it.isNotBlank() }
        .toList()
}