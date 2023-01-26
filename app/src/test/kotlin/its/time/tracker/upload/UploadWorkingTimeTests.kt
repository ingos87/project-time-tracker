package its.time.tracker.upload

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import its.time.tracker.*

class UploadWorkingTimeTests : FunSpec({

    beforeEach {
        ensureTestConfig()
        ensureCsvEmpty()
    }

    test("recording working time is not possible if there is no config file") {
        ensureNoConfig()

        val output = tapSystemOut {
            main(arrayOf("timekeeping", "-w2023-01"))
        }

        output shouldStartWith "No config file found in ./app.json\n" +
                "Use 'java -jar app.jar init' with the according parameters"
    }

    test("prints compliant working times") {
        // TUE
        executeClockInWitArgs(arrayOf("-tcow",      "--datetime=2023-08-22 07:30"))
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2023-08-22 09:00"))
        executeClockInWitArgs(arrayOf("-tjourfixe", "--datetime=2023-08-22 14:00"))
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2023-08-22 15:00"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2023-08-22 15:30"))

        // WED
        executeClockInWitArgs(arrayOf("-tcow",      "--datetime=2023-08-23 04:45"))
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2023-08-23 09:00"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2023-08-23 14:45"))
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2023-08-23 15:15"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2023-08-23 15:30"))

        // FRI
        executeClockInWitArgs(arrayOf("-tcow",      "--datetime=2023-08-25 08:45"))
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2023-08-25 09:00"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2023-08-25 14:45"))
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2023-08-25 15:15"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2023-08-25 15:30"))
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2023-08-25 16:00"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2023-08-25 16:30"))

        // SAT - no work
        executeClockInWitArgs(arrayOf("-tcow",      "--datetime=2023-08-26 07:30"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2023-08-26 09:00"))

        val output = tapSystemOut {
            executeUploadWorkingTimeWitArgs(arrayOf("-w2023-34"))
        }

        splitIgnoreBlank(output) shouldBe listOf(
            " date       │ compliant values            ║ original values",
            "────────────┼─────────────┼───────────────╬─────────────┼───────",
            " 2023-08-22 │ 07:30-16:00 │ 08:00 ( 8,00) ║ 07:30-15:30 │ 08:00",
            " 2023-08-23 │ 06:00-16:45 │ 10:00 (10,00) ║ 04:45-15:30 │ 10:15",
            " 2023-08-25 │ 08:45-17:45 │ 08:30 ( 8,50) ║ 08:45-16:30 │ 06:45",
            " 2023-08-26 │             │ 00:00 ( 0,00) ║ 07:30-09:00 │ 01:30",
            "NOOP mode. Uploaded nothing")
    }

})
