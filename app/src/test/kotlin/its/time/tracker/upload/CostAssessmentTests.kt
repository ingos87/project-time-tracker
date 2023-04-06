package its.time.tracker.upload

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import its.time.tracker.*
import its.time.tracker.util.DATE_PATTERN
import its.time.tracker.util.DateTimeUtil
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.MINUTES

class CostAssessmentTests : FunSpec({

    beforeEach {
        ensureTestConfig()
        ensureCsvEmpty()
    }

    test("cost assessment is not possible if there is no config file") {
        executeClockInWitArgs(arrayOf("--project=ProjectA", "-tEPP-007",  "--datetime=2022-01-03 07:30"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-01-03 16:30"))

        ensureNoConfig()

        val output = tapSystemOut {
            main(arrayOf("cost-assessment", "-w2022-01"))
        }

        output shouldStartWith "No config file found in ./app.json\n" +
                "Use 'java -jar app.jar init' with the according parameters"
    }

    test("cost assessment shows err message if there are not clock-in events") {
        executeClockOutWitArgs(arrayOf("--datetime=2023-01-02 14:30"))

        val output = tapSystemOut {
            executeCostAssessmentWitArgs(arrayOf("-w2023-01"))
        }

        splitIgnoreBlank(output) shouldBe listOf(
            "[NO SUMMARY for 2023-01-02 - 2023-01-08 because there are no clock-in events]",
            "NOOP mode. Uploaded nothing")
    }

    test("cost assessment for standard week") {
        // MON (public holiday)
        executeClockInWitArgs(arrayOf("--project=ProjectA", "-tEPP-007",  "--datetime=2023-05-01 12:00"))
        executeClockOutWitArgs(arrayOf("--datetime=2023-05-01 14:55"))

        // TUE
        executeClockInWitArgs(arrayOf("-pProjectA", "-tEPP-007",  "--datetime=2023-05-02 07:00"))
        executeClockInWitArgs(arrayOf("-pProjectB", "-tEPP-009",  "--datetime=2023-05-02 08:00"))
        executeClockInWitArgs(arrayOf("-pDoD", "-tcoww",     "--datetime=2023-05-02 09:45"))
        executeClockInWitArgs(arrayOf("-pWartung", "-tEDF-1",    "--datetime=2023-05-02 10:05"))
        executeClockOutWitArgs(arrayOf("--datetime=2023-05-02 12:00"))
        executeClockInWitArgs(arrayOf("-pWartung", "-tEDF-1",    "--datetime=2023-05-02 13:00"))
        executeClockOutWitArgs(arrayOf("--datetime=2023-05-02 13:25"))

        // WED
        executeClockInWitArgs(arrayOf("--project=ProjectA", "-tEPP-008",  "--datetime=2023-05-03 08:00"))
        executeClockOutWitArgs(arrayOf("--datetime=2023-05-03 19:00"))

        // FRI
        executeClockInWitArgs(arrayOf("--project=ProjectA", "-tEPP-008",  "--datetime=2023-05-05 08:00"))
        executeClockOutWitArgs(arrayOf("--datetime=2023-05-05 13:03"))

        // SAT
        executeClockInWitArgs(arrayOf("-pWartung", "-tEDF-9",    "--datetime=2023-05-06 08:00"))
        executeClockOutWitArgs(arrayOf("--datetime=2023-05-06 10:10"))


        val output = tapSystemOut {
            executeCostAssessmentWitArgs(arrayOf("-w2023-18"))
        }

        splitIgnoreBlank(output) shouldBe listOf(
            "[SUMMARY for 2023-05-01 - 2023-05-07]",
            "┌──────────────────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┐",
            "│ weekday          │  MON │  TUE │  WED │  THU │  FRI │  SAT │  SUN │",
            "│ day of month     │    1 │    2 │    3 │    4 │    5 │    6 │    7 │",
            "├──────────────────┼──────┼──────┼──────┼──────┼──────┼──────┼──────┤",
            "│ Other absence    │  8,00│      │      │  8,00│      │      │      │",
            "│ ProjectA         │      │  4,00│ 11,00│      │  5,00│      │      │",
            "│ ProjectB         │      │  2,00│      │      │      │      │      │",
            "│ DoD              │      │  0,50│      │      │      │      │      │",
            "│ Wartung          │      │  2,50│      │      │  2,00│      │      │",
            "└──────────────────┴──────┴──────┴──────┴──────┴──────┴──────┴──────┘",
            "NOOP mode. Uploaded nothing")
    }
})

