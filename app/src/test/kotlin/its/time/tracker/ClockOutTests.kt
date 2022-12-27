package its.time.tracker

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErrAndOut
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val TEST_CSV_PATH = "/Users/tollpatsch/test_its_times.csv"

class ClockOutTests : FunSpec({

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

    test("clock-out is saved with current time") {
        val output = tapSystemOut {
            executeClockOutWitArgs(emptyArray())
        }

        output shouldStartWith "clock-out saved: 20"
        output.length shouldBeExactly 31
    }

    test("clock-out is saved with manual time") {
        val output = tapSystemOut {
            executeClockOutWitArgs(arrayOf<String>("--datetime=20221223_1730"))
        }

        output shouldBe "clock-out saved: 20221223_1730\n"
    }

    test("clock-out is saved with today's date if only time is given") {
        val output = tapSystemOut {
            executeClockOutWitArgs(arrayOf<String>("-d1645"))
        }

        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneId.systemDefault())
        val today = formatter.format(Instant.now())

        output shouldBe "clock-out saved: ${today}_1645\n"
    }

    test("clock-out is discarded if date is invalid") {
        val output = tapSystemErrAndOut {
            executeClockOutWitArgs(arrayOf<String>("-d20200132"))
        }

        output shouldBe "invalid datetime input '20200132'\n"
    }

    test("clock-out is discarded if time is invalid") {
        val output = tapSystemErrAndOut {
            executeClockOutWitArgs(arrayOf<String>("-d1961"))
        }

        output shouldBe "invalid datetime input '1961'\n"
    }

    test("clock-out is saved empty csv") {
        val output = tapSystemOut {
            executeClockOutWitArgs(arrayOf<String>("-v", "--datetime=20221223_1730"))
        }

        output shouldBe "nothing found at /Users/tollpatsch/test_its_times.csv. Will create new csv file in the process\n" +
                "wrote 1 events to /Users/tollpatsch/test_its_times.csv\n" +
                "clock-out saved: 20221223_1730\n"
    }
})

fun executeClockOutWitArgs(args: Array<String>) {
    main(arrayOf<String>("clock-out", "--csvpath=$TEST_CSV_PATH").plus(args))
}