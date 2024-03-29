package project.time.tracker.service

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErrAndOut
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import project.time.tracker.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ClockInTests : FunSpec({

    beforeEach {
        ensureTestConfig("", "", "")
        ensureCsvEmpty()
    }

    test("clock-in is not possible if there is no config file") {
        ensureNoConfig()

        val output = tapSystemOut {
            project.time.tracker.main(arrayOf("clock-in", "-pwartung"))
        }

        output shouldStartWith "No config file found in ./app.json\n" +
                "Use 'java -jar app.jar init' with the according parameters"
    }

    test("clock-in is saved with current time") {
        val output = tapSystemOut {
            executeClockInWitArgs(arrayOf("-pwartung"))
        }

        output shouldStartWith "clock-in for project 'wartung', topic 'null', story 'null' saved: 20"
        output.length shouldBeExactly 83
    }

    test("clock-in is saved with manual time") {
        val output = tapSystemOut {
            executeClockInWitArgs(arrayOf("-pwartung", "-tEPP-007", "--datetime=2022-12-23 17:30"))
        }

        output shouldBe "clock-in for project 'wartung', topic 'EPP-007', story 'null' saved: 2022-12-23 17:30\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;project;topic;story",
            "2022-12-23 17:30;CLOCK_IN;wartung;EPP-007;")
    }

    test("clock-in is saved with today's date if only time is given") {
        val output = tapSystemOut {
            executeClockInWitArgs(arrayOf("-pwartung", "-tEPP-007", "-d05:34"))
        }

        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault())
        val today = formatter.format(Instant.now())

        output shouldBe "clock-in for project 'wartung', topic 'EPP-007', story 'null' saved: $today 05:34\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;project;topic;story",
            "$today 05:34;CLOCK_IN;wartung;EPP-007;")
    }

    test("clock-in is discarded if date is invalid") {
        val output = tapSystemErrAndOut {
            executeClockInWitArgs(arrayOf("-pwartung", "-d2020-01-32"))
        }

        output shouldBe "unable to parse '2020-01-32' for pattern 'uuuu-MM-dd HH:mm'\n"
    }

    test("clock-in is discarded if time is invalid") {
        val output = tapSystemErrAndOut {
            executeClockInWitArgs(arrayOf("-pwartung", "-d1961"))
        }

        output shouldBe "unable to parse '1961' for pattern 'uuuu-MM-dd HH:mm'\n"
    }

    test("clock-in can be overwritten") {
        executeClockInWitArgs(arrayOf("-pwartung", "-tEPP-007", "--datetime=2022-12-23 17:30"))
        val output = tapSystemOut {
            executeClockInWitArgs(arrayOf("-pfeature", "-tEPP-123", "--datetime=2022-12-23 17:30"))
        }

        output shouldBe "Will overwrite current event with identical time stamp: ClockEvent(dateTime=2022-12-23T17:30, eventType=CLOCK_IN, project=wartung, topic=EPP-007, story=)\n" +
                "clock-in for project 'feature', topic 'EPP-123', story 'null' saved: 2022-12-23 17:30\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;project;topic;story",
            "2022-12-23 17:30;CLOCK_IN;feature;EPP-123;")
    }

    test("cannot overwrite clock-out with clock-in") {
        executeClockOutWitArgs(arrayOf("--datetime=2022-12-23 17:30"))
        val output = tapSystemOut {
            executeClockInWitArgs(arrayOf("-pwartung", "-tEPP-007", "--datetime=2022-12-23 17:30"))
        }

        output shouldBe "Cannot overwrite event of different type. You must remove the present event before.\n" +
                "present: ClockEvent(dateTime=2022-12-23T17:30, eventType=CLOCK_OUT, project=, topic=MANUAL_CLOCK_OUT, story=)\n" +
                "new    : ClockEvent(dateTime=2022-12-23T17:30, eventType=CLOCK_IN, project=wartung, topic=EPP-007, story=)\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;project;topic;story",
            "2022-12-23 17:30;CLOCK_OUT;;MANUAL_CLOCK_OUT;")
    }
})

