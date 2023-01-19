package its.time.tracker.service

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErrAndOut
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import its.time.tracker.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ClockInTests : FunSpec({

    beforeEach {
        ensureTestConfig()
        ensureCsvEmpty()
    }

    test("clock-in is not possible if there is no config file") {
        ensureNoConfig()

        val output = tapSystemOut {
            main(arrayOf("clock-in", "-tEPP-007"))
        }

        output shouldStartWith "No config file found in ./app.json\n" +
                "Use 'java -jar app.jar init' with the according parameters"
    }

    test("clock-in is saved with current time") {
        val output = tapSystemOut {
            executeClockInWitArgs(arrayOf("-tEPP-007"))
        }

        output shouldStartWith "clock-in for topic 'EPP-007' saved: 20"
        output.length shouldBeExactly 53
    }

    test("clock-in is saved with manual time") {
        val output = tapSystemOut {
            executeClockInWitArgs(arrayOf("-tEPP-007", "--datetime=2022-12-23 17:30"))
        }

        output shouldBe "clock-in for topic 'EPP-007' saved: 2022-12-23 17:30\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;topic",
            "2022-12-23 17:30;CLOCK_IN;EPP-007")
    }

    test("clock-in is saved with today's date if only time is given") {
        val output = tapSystemOut {
            executeClockInWitArgs(arrayOf("-tEPP-007", "-d05:34"))
        }

        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault())
        val today = formatter.format(Instant.now())

        output shouldBe "clock-in for topic 'EPP-007' saved: $today 05:34\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;topic",
            "$today 05:34;CLOCK_IN;EPP-007")
    }

    test("clock-in is discarded if date is invalid") {
        val output = tapSystemErrAndOut {
            executeClockInWitArgs(arrayOf("-tEPP-007", "-d2020-01-32"))
        }

        output shouldBe "unable to parse '2020-01-32' for pattern 'uuuu-MM-dd HH:mm'\n"
    }

    test("clock-in is discarded if time is invalid") {
        val output = tapSystemErrAndOut {
            executeClockInWitArgs(arrayOf("-tEPP-007", "-d1961"))
        }

        output shouldBe "unable to parse '1961' for pattern 'uuuu-MM-dd HH:mm'\n"
    }

    test("clock-in can be overwritten") {
        executeClockInWitArgs(arrayOf("-tEPP-007", "--datetime=2022-12-23 17:30"))
        val output = tapSystemOut {
            executeClockInWitArgs(arrayOf("-tEPP-123", "--datetime=2022-12-23 17:30"))
        }

        output shouldBe "Will overwrite current event with identical time stamp: ClockEvent(dateTime=2022-12-23T17:30, eventType=CLOCK_IN, topic=EPP-007)\n" +
                "clock-in for topic 'EPP-123' saved: 2022-12-23 17:30\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;topic",
            "2022-12-23 17:30;CLOCK_IN;EPP-123")
    }

    test("cannot overwrite clock-out with clock-in") {
        executeClockOutWitArgs(arrayOf("--datetime=2022-12-23 17:30"))
        val output = tapSystemOut {
            executeClockInWitArgs(arrayOf("-tEPP-007", "--datetime=2022-12-23 17:30"))
        }

        output shouldBe "Cannot overwrite event of different type. You must remove the present event before.\n" +
                "present: ClockEvent(dateTime=2022-12-23T17:30, eventType=CLOCK_OUT, topic=MANUAL_CLOCK_OUT)\n" +
                "new    : ClockEvent(dateTime=2022-12-23T17:30, eventType=CLOCK_IN, topic=EPP-007)\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;topic",
            "2022-12-23 17:30;CLOCK_OUT;MANUAL_CLOCK_OUT")
    }
})

