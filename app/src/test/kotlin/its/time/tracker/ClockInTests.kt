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

class ClockInTests : FunSpec({

    beforeEach {
        ensureCsvEmpty()
    }

    test("clock-in is saved with current time") {
        val output = tapSystemOut {
            executeClockInWitArgs(arrayOf<String>("-tEPP-007"))
        }

        output shouldStartWith "clock-in for topic 'EPP-007' saved: 20"
        output.length shouldBeExactly 50
    }

    test("clock-in is saved with manual time") {
        val output = tapSystemOut {
            executeClockInWitArgs(arrayOf<String>("-tEPP-007", "--datetime=20221223_1730"))
        }

        output shouldBe "clock-in for topic 'EPP-007' saved: 20221223_1730\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;topic",
            "20221223_1730;CLOCK_IN;EPP-007")
    }

    test("clock-in is saved with today's date if only time is given") {
        val output = tapSystemOut {
            executeClockInWitArgs(arrayOf<String>("-tEPP-007", "-d0534"))
        }

        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneId.systemDefault())
        val today = formatter.format(Instant.now())

        output shouldBe "clock-in for topic 'EPP-007' saved: ${today}_0534\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;topic",
            "${today}_0534;CLOCK_IN;EPP-007")
    }

    test("clock-in is discarded if date is invalid") {
        val output = tapSystemErrAndOut {
            executeClockInWitArgs(arrayOf<String>("-tEPP-007", "-d20200132"))
        }

        output shouldBe "invalid datetime input '20200132'\n"
    }

    test("clock-in is discarded if time is invalid") {
        val output = tapSystemErrAndOut {
            executeClockInWitArgs(arrayOf<String>("-tEPP-007", "-d1961"))
        }

        output shouldBe "invalid datetime input '1961'\n"
    }

    test("clock-in can be overwritten") {
        executeClockInWitArgs(arrayOf<String>("-tEPP-007", "--datetime=20221223_1730"))
        val output = tapSystemOut {
            executeClockInWitArgs(arrayOf<String>("-tEPP-123", "--datetime=20221223_1730"))
        }

        output shouldBe "Will overwrite current event with identical time stamp: ClockEvent(dateTime=20221223_1730, eventType=CLOCK_IN, topic=EPP-007)\n" +
                "clock-in for topic 'EPP-123' saved: 20221223_1730\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;topic",
            "20221223_1730;CLOCK_IN;EPP-123")
    }

    test("cannot overwrite clock-out with clock-in") {
        executeClockOutWitArgs(arrayOf<String>("--datetime=20221223_1730"))
        val output = tapSystemOut {
            executeClockInWitArgs(arrayOf<String>("-tEPP-007", "--datetime=20221223_1730"))
        }

        output shouldBe "Cannot overwrite event of different type. You must remove the present event before.\n" +
                "present: ClockEvent(dateTime=20221223_1730, eventType=CLOCK_OUT, topic=MANUAL_CLOCK_OUT)\n" +
                "new    : ClockEvent(dateTime=20221223_1730, eventType=CLOCK_IN, topic=EPP-007)\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;topic",
            "20221223_1730;CLOCK_OUT;MANUAL_CLOCK_OUT")
    }
})

