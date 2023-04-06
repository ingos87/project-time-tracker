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

class ClockOutTests : FunSpec({

    beforeEach {
        ensureTestConfig()
        ensureCsvEmpty()
    }

    test("clock-out is not possible if there is no config file") {
        ensureNoConfig()

        val output = tapSystemOut {
            main(arrayOf("clock-out"))
        }

        output shouldStartWith "No config file found in ./app.json\n" +
                "Use 'java -jar app.jar init' with the according parameters"
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
            executeClockOutWitArgs(arrayOf("--datetime=2022-12-23 17:30"))
        }

        output shouldBe "clock-out saved: 2022-12-23 17:30\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;project;topic",
            "2022-12-23 17:30;CLOCK_OUT;;MANUAL_CLOCK_OUT")
    }

    test("clock-out is saved with today's date if only time is given") {
        val output = tapSystemOut {
            executeClockOutWitArgs(arrayOf("-d16:45"))
        }

        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault())
        val today = formatter.format(Instant.now())

        output shouldBe "clock-out saved: $today 16:45\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;project;topic",
            "$today 16:45;CLOCK_OUT;;MANUAL_CLOCK_OUT")
    }

    test("clock-out is discarded if date is invalid") {
        val output = tapSystemErrAndOut {
            executeClockOutWitArgs(arrayOf("-d2020-01-32"))
        }

        output shouldBe "unable to parse '2020-01-32' for pattern 'uuuu-MM-dd HH:mm'\n"
    }

    test("clock-out is discarded if time is invalid") {
        val output = tapSystemErrAndOut {
            executeClockOutWitArgs(arrayOf("-d19:61"))
        }

        output shouldBe "unable to parse '19:61' for pattern 'uuuu-MM-dd HH:mm'\n"
    }

    test("clock-out is saved empty csv") {
        val output = tapSystemOut {
            executeClockOutWitArgs(arrayOf("-v", "--datetime=2022-12-23 17:30"))
        }

        output shouldBe "loaded 0 clock events from /tmp/its-time-tracker/test_its_times.csv\n" +
                "wrote 1 events to /tmp/its-time-tracker/test_its_times.csv\n" +
                "clock-out saved: 2022-12-23 17:30\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;project;topic",
            "2022-12-23 17:30;CLOCK_OUT;;MANUAL_CLOCK_OUT")
    }

    test("clock-out can be overwritten") {
        executeClockOutWitArgs(arrayOf("--datetime=2022-12-23 17:30"))
        val output = tapSystemOut {
            executeClockOutWitArgs(arrayOf("--datetime=2022-12-23 17:30"))
        }

        output shouldBe "Will overwrite current event with identical time stamp: ClockEvent(dateTime=2022-12-23T17:30, eventType=CLOCK_OUT, project=, topic=MANUAL_CLOCK_OUT)\n" +
                "clock-out saved: 2022-12-23 17:30\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;project;topic",
            "2022-12-23 17:30;CLOCK_OUT;;MANUAL_CLOCK_OUT")
    }

    test("cannot overwrite clock-in with clock-out") {
        executeClockInWitArgs(arrayOf("-pwartung", "-tEPP-007", "--datetime=2022-12-23 17:30"))
        val output = tapSystemOut {
            executeClockOutWitArgs(arrayOf("--datetime=2022-12-23 17:30"))
        }

        output shouldBe "Cannot overwrite event of different type. You must remove the present event before.\n" +
                "present: ClockEvent(dateTime=2022-12-23T17:30, eventType=CLOCK_IN, project=wartung, topic=EPP-007)\n" +
                "new    : ClockEvent(dateTime=2022-12-23T17:30, eventType=CLOCK_OUT, project=, topic=MANUAL_CLOCK_OUT)\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;project;topic",
            "2022-12-23 17:30;CLOCK_IN;wartung;EPP-007")
    }
})