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
        output.length shouldBeExactly 34
    }

    test("clock-out is saved with manual time") {
        val output = tapSystemOut {
            executeClockOutWitArgs(arrayOf<String>("--datetime=2022-12-23 17:30"))
        }

        output shouldBe "clock-out saved: 2022-12-23 17:30\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;topic",
            "2022-12-23 17:30;CLOCK_OUT;MANUAL_CLOCK_OUT")
    }

    test("clock-out is saved with today's date if only time is given") {
        val output = tapSystemOut {
            executeClockOutWitArgs(arrayOf<String>("-d16:45"))
        }

        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault())
        val today = formatter.format(Instant.now())

        output shouldBe "clock-out saved: ${today} 16:45\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;topic",
            "${today} 16:45;CLOCK_OUT;MANUAL_CLOCK_OUT")
    }

    test("clock-out is discarded if date is invalid") {
        val output = tapSystemErrAndOut {
            executeClockOutWitArgs(arrayOf<String>("-d2020-01-32"))
        }

        output shouldBe "unable to parse '2020-01-32' for pattern 'uuuu-MM-dd HH:mm'\n"
    }

    test("clock-out is discarded if time is invalid") {
        val output = tapSystemErrAndOut {
            executeClockOutWitArgs(arrayOf<String>("-d19:61"))
        }

        output shouldBe "unable to parse '19:61' for pattern 'uuuu-MM-dd HH:mm'\n"
    }

    test("clock-out is saved empty csv") {
        val output = tapSystemOut {
            executeClockOutWitArgs(arrayOf<String>("-v", "--datetime=2022-12-23 17:30"))
        }

        output shouldBe "loaded 0 clock events from /Users/tollpatsch/test_its_times.csv\n" +
                "wrote 1 events to /Users/tollpatsch/test_its_times.csv\n" +
                "clock-out saved: 2022-12-23 17:30\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;topic",
            "2022-12-23 17:30;CLOCK_OUT;MANUAL_CLOCK_OUT")
    }

    test("clock-out can be overwritten") {
        executeClockOutWitArgs(arrayOf<String>("--datetime=2022-12-23 17:30"))
        val output = tapSystemOut {
            executeClockOutWitArgs(arrayOf<String>("--datetime=2022-12-23 17:30"))
        }

        output shouldBe "Will overwrite current event with identical time stamp: ClockEvent(dateTime=2022-12-23T17:30, eventType=CLOCK_OUT, topic=MANUAL_CLOCK_OUT)\n" +
                "clock-out saved: 2022-12-23 17:30\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;topic",
            "2022-12-23 17:30;CLOCK_OUT;MANUAL_CLOCK_OUT")
    }

    test("cannot overwrite clock-in with clock-out") {
        executeClockInWitArgs(arrayOf<String>("-tEPP-007", "--datetime=2022-12-23 17:30"))
        val output = tapSystemOut {
            executeClockOutWitArgs(arrayOf<String>("--datetime=2022-12-23 17:30"))
        }

        output shouldBe "Cannot overwrite event of different type. You must remove the present event before.\n" +
                "present: ClockEvent(dateTime=2022-12-23T17:30, eventType=CLOCK_IN, topic=EPP-007)\n" +
                "new    : ClockEvent(dateTime=2022-12-23T17:30, eventType=CLOCK_OUT, topic=MANUAL_CLOCK_OUT)\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;topic",
            "2022-12-23 17:30;CLOCK_IN;EPP-007")
    }
})