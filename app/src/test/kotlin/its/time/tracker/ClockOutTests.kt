package its.time.tracker

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErrAndOut
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ClockOutTests : FunSpec({

    beforeEach {
        ensureCsvEmpty()
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
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;topic",
            "20221223_1730;CLOCK_OUT;MANUAL_CLOCK_OUT")
    }

    test("clock-out is saved with today's date if only time is given") {
        val output = tapSystemOut {
            executeClockOutWitArgs(arrayOf<String>("-d1645"))
        }

        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneId.systemDefault())
        val today = formatter.format(Instant.now())

        output shouldBe "clock-out saved: ${today}_1645\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;topic",
            "${today}_1645;CLOCK_OUT;MANUAL_CLOCK_OUT")
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

        output shouldBe "loaded 0 clock events from /Users/tollpatsch/test_its_times.csv\n" +
                "wrote 1 events to /Users/tollpatsch/test_its_times.csv\n" +
                "clock-out saved: 20221223_1730\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;topic",
            "20221223_1730;CLOCK_OUT;MANUAL_CLOCK_OUT")
    }
})