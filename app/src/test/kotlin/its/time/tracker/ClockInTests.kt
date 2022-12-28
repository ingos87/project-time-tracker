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
        getTimesCsvContent() shouldBe listOf("20221223_1730;CLOCK_IN;EPP-007;")
    }

    test("clock-in is saved with today's date if only time is given") {
        val output = tapSystemOut {
            executeClockInWitArgs(arrayOf<String>("-tEPP-007", "-d0534"))
        }

        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneId.systemDefault())
        val today = formatter.format(Instant.now())

        output shouldBe "clock-in for topic 'EPP-007' saved: ${today}_0534\n"
        getTimesCsvContent() shouldBe listOf("${today}_0534;CLOCK_IN;EPP-007;")
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
})

