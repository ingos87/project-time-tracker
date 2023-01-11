package its.time.tracker

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FlexTimeTests : FunSpec({

    beforeEach {
        ensureTestConfig()
        ensureCsvEmpty()
    }

    test("flex-time is not possible if there is no config file") {
        ensureNoConfig()

        val output = tapSystemOut {
            main(arrayOf<String>("flex-time"))
        }

        output shouldStartWith "No config file found in ./app.json\n" +
                "Use 'java -jar app.jar init' with the according parameters"
    }

    test("flex-time is saved with current day") {
        val output = tapSystemOut {
            executeFlexTimeWitArgs(emptyArray())
        }
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault())
        val today = formatter.format(Instant.now())

        splitIgnoreBlank(output) shouldBe listOf(
            "Flex time saved for $today",
            "Note that now, you can no longer do clock-ins for this day.")
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;topic",
            "$today 00:00;FLEX_TIME;")
    }

    test("flex-time is declined if clock-in is present on same day") {
        executeClockInWitArgs(arrayOf<String>("-tEPP-007", "--datetime=2022-12-23 17:30"))

        val output = tapSystemOut {
            executeFlexTimeWitArgs(arrayOf<String>("--date=2022-12-23"))
        }

        output shouldBe "Flex time is not possible for 2022-12-23 due to existing clock-in(s)\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;topic",
            "2022-12-23 17:30;CLOCK_IN;EPP-007")
    }

    test("clock-in is declined after flex-time was saved for the same day") {
        executeFlexTimeWitArgs(arrayOf<String>("--date=2022-12-23"))

        val output = tapSystemOut {
            executeClockInWitArgs(arrayOf<String>("-tEPP-007", "--datetime=2022-12-23 07:30"))
        }

        output shouldBe "Clock-in is not possible on 2022-12-23 due to present flex time.\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;topic",
            "2022-12-23 00:00;FLEX_TIME;")
    }

    test("flex-time saved if only clock-out is present on same day") {
        // because that clock-out might actually mark the previous work day's end

        executeClockOutWitArgs(arrayOf<String>("--datetime=2022-12-23 00:23"))

        val output = tapSystemOut {
            executeFlexTimeWitArgs(arrayOf<String>("--date=2022-12-23"))
        }

        splitIgnoreBlank(output) shouldBe listOf(
            "Flex time saved for 2022-12-23",
            "Note that now, you can no longer do clock-ins for this day.")
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;topic",
            "2022-12-23 00:00;FLEX_TIME;",
            "2022-12-23 00:23;CLOCK_OUT;MANUAL_CLOCK_OUT")
    }

    test("clock-out is still possible after flex-time was saved on same day") {
        // because that clock-out might actually mark the previous work day's end

        executeFlexTimeWitArgs(arrayOf<String>("--date=2022-12-23"))

        val output = tapSystemOut {
            executeClockOutWitArgs(arrayOf<String>("--datetime=2022-12-23 00:23"))
        }

        output shouldBe "clock-out saved: 2022-12-23 00:23\n"
        getTimesCsvContent() shouldBe listOf(
            "dateTime;eventType;topic",
            "2022-12-23 00:00;FLEX_TIME;",
            "2022-12-23 00:23;CLOCK_OUT;MANUAL_CLOCK_OUT")
    }
})