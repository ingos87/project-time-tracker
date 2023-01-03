package its.time.tracker

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.junit.platform.commons.util.StringUtils

class SummaryTests : FunSpec({

    beforeEach {
        ensureCsvEmpty()
    }

    test("simple clock-in and clock-out") {
        executeClockInWitArgs(arrayOf<String>("-tEPP-007",  "--datetime=2022-01-03 07:30"))
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-01-03 16:30"))

        val output = tapSystemOut {
            executeDailySummaryWitArgs(arrayOf<String>("-d2022-01-03"))
        }

        splitIgnoreBlank(output) shouldBe listOf(
                "[SUMMARY for 2022-01-03]",
                "┌────────────────────────────────────────────────┐",
                "│ clock-in:         07:30                        │",
                "│ clock-out:        16:30                        │",
                "├────────────────────────────────────────────────┤",
                "│ total work time:  09:00                        │",
                "│ total break time: 00:00                        │",
                "├════════════════════════════════════════════════┤",
                "│ ProjectA:     09:00  (EPP-007)                 │",
                "└────────────────────────────────────────────────┘")
    }

    test("day with breaks and several projects") {
        executeClockInWitArgs(arrayOf<String>("-tEPP-007",  "--datetime=2022-01-03 07:30"))
        executeClockInWitArgs(arrayOf<String>("-tEPP-008",  "--datetime=2022-01-03 09:00")) // worktime 1:30
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-01-03 11:25")) // worktime 3:55
        executeClockInWitArgs(arrayOf<String>("-tEPP-123",  "--datetime=2022-01-03 13:30")) // break 2:05
        executeClockInWitArgs(arrayOf<String>("-tEPP-009",  "--datetime=2022-01-03 13:40")) // worktime 0:10
        executeClockInWitArgs(arrayOf<String>("-tEPP-0815", "--datetime=2022-01-03 13:50")) // worktime 0:10
        executeClockInWitArgs(arrayOf<String>("-tEPP-17662","--datetime=2022-01-03 14:00")) // worktime 0:10
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-01-03 15:30")) // worktime 1:30
        executeClockInWitArgs(arrayOf<String>("-tallhands", "--datetime=2022-01-03 17:05")) // break 1:35
        executeClockInWitArgs(arrayOf<String>("-tEDF-99",   "--datetime=2022-01-03 18:05")) // worktime 1:00
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-01-03 20:52")) // worktime 2:47

        val output = tapSystemOut {
            executeDailySummaryWitArgs(arrayOf<String>("-d2022-01-03"))
        }

        splitIgnoreBlank(output) shouldBe listOf(
                "[SUMMARY for 2022-01-03]",
                "┌────────────────────────────────────────────────┐",
                "│ clock-in:         07:30                        │",
                "│ clock-out:        20:52                        │",
                "├────────────────────────────────────────────────┤",
                "│ total work time:  09:42                        │",
                "│ total break time: 03:40                        │",
                "├════════════════════════════════════════════════┤",
                "│ ProjectA:     03:55  (EPP-007,EPP-008)         │",
                "│ ProjectB:     02:00  (EPP-123,EPP-009,EPP-0815)│",
                "│ ITS meetings: 01:00  (allhands)                │",
                "│ EDF-99:       02:47  (EDF-99)                  │",
                "└────────────────────────────────────────────────┘")
    }

    test("missing clock-out is set beyond max working hours per day") {
        executeClockInWitArgs(arrayOf<String>("-tEPP-007",  "--datetime=2022-01-03 07:30"))
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-01-03 16:30"))
        executeClockInWitArgs(arrayOf<String>("-tEPP-007",  "--datetime=2022-01-03 17:30"))

        val output = tapSystemOut {
            executeDailySummaryWitArgs(arrayOf<String>("-d2022-01-03"))
        }

        splitIgnoreBlank(output) shouldBe listOf(
                "2022-01-03: No final clock-out found. Will insert one to fill up working time to 09:30 hours.",
                "[SUMMARY for 2022-01-03]",
                "┌────────────────────────────────────────────────┐",
                "│ clock-in:         07:30                        │",
                "│ clock-out:        18:00                        │",
                "├────────────────────────────────────────────────┤",
                "│ total work time:  09:30                        │",
                "│ total break time: 01:00                        │",
                "├════════════════════════════════════════════════┤",
                "│ ProjectA:     09:30  (EPP-007)                 │",
                "└────────────────────────────────────────────────┘")
    }

    test("missing clock-out is set to max working hours per day") {
        executeClockInWitArgs(arrayOf<String>("-tEPP-007",  "--datetime=2022-01-03 09:30"))
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-01-03 16:30"))
        executeClockInWitArgs(arrayOf<String>("-tEPP-007",  "--datetime=2022-01-03 17:30"))

        val output = tapSystemOut {
            executeDailySummaryWitArgs(arrayOf<String>("-d2022-01-03"))
        }

        splitIgnoreBlank(output) shouldBe listOf(
                "2022-01-03: No final clock-out found. Will insert one to fill up working time to 09:00 hours.",
                "[SUMMARY for 2022-01-03]",
                "┌────────────────────────────────────────────────┐",
                "│ clock-in:         09:30                        │",
                "│ clock-out:        19:30                        │",
                "├────────────────────────────────────────────────┤",
                "│ total work time:  09:00                        │",
                "│ total break time: 01:00                        │",
                "├════════════════════════════════════════════════┤",
                "│ ProjectA:     09:00  (EPP-007)                 │",
                "└────────────────────────────────────────────────┘")
    }

    test("monthly summary for one day with lunch break") {
        executeClockInWitArgs(arrayOf<String>("-tEPP-007",  "--datetime=2022-11-02 07:30"))
        executeClockInWitArgs(arrayOf<String>("-tEPP-009",  "--datetime=2022-11-02 08:20"))
        executeClockInWitArgs(arrayOf<String>("-tcow",      "--datetime=2022-11-02 09:45"))
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-11-02 12:00"))
        executeClockInWitArgs(arrayOf<String>("-tEDF-0815", "--datetime=2022-11-02 13:15"))
        executeClockInWitArgs(arrayOf<String>("-tEDF-1234", "--datetime=2022-11-02 13:30"))
        executeClockInWitArgs(arrayOf<String>("-tDVR-7",    "--datetime=2022-11-02 14:00"))
        executeClockInWitArgs(arrayOf<String>("-tf2f",      "--datetime=2022-11-02 15:00"))
        executeClockInWitArgs(arrayOf<String>("-tDVR-7",    "--datetime=2022-11-02 15:45"))
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-11-02 16:45"))

        val output = tapSystemOut {
            executeMonthlySummaryWitArgs(arrayOf<String>("-m2022-11"))
        }

        splitIgnoreBlank(output) shouldBe listOf(
                "[SUMMARY for 2022-11]",
                "┌──────────────┬──────┐",
                "│ day of month │    2 │",
                "│ weekday      │  WED │",
                "├──────────────┼──────┤",
                "│ clock-in     │ 07:30│",
                "│ clock-out    │ 16:45│",
                "├══════════════┼══════┤",
                "│ ProjectA     │ 00:50│",
                "│ ProjectB     │ 01:25│",
                "│ DoD          │ 02:15│",
                "│ EDF-0815     │ 00:15│",
                "│ EDF-1234     │ 00:30│",
                "│ ITS meetings │ 00:45│",
                "│ Recruiting   │ 02:00│",
                "├──────────────┼──────┤",
                "│ total        │ 08:00│",
                "└──────────────┴──────┘")
    }

    test("monthly summary") {
        // NOV-01
        executeClockInWitArgs(arrayOf<String>("-tcow",      "--datetime=2022-11-01 07:30"))
        executeClockInWitArgs(arrayOf<String>("-tEPP-007",  "--datetime=2022-11-01 09:00"))
        executeClockInWitArgs(arrayOf<String>("-tjourfixe", "--datetime=2022-11-01 14:00"))
        executeClockInWitArgs(arrayOf<String>("-tEPP-007",  "--datetime=2022-11-01 15:00"))
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-11-01 17:30"))

        // NOV-02
        executeClockInWitArgs(arrayOf<String>("-tEPP-007",  "--datetime=2022-11-02 07:45"))
        executeClockInWitArgs(arrayOf<String>("-tEPP-123",  "--datetime=2022-11-02 09:45"))
        executeClockInWitArgs(arrayOf<String>("-tEDF-2223", "--datetime=2022-11-02 10:45"))
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-11-02 12:00"))
        executeClockInWitArgs(arrayOf<String>("-tEDF-2223", "--datetime=2022-11-02 13:15"))
        executeClockInWitArgs(arrayOf<String>("-tDVR-3",    "--datetime=2022-11-02 14:00"))
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-11-02 17:30"))

        // NOV-03
        executeClockInWitArgs(arrayOf<String>("-tEPP-007",  "--datetime=2022-11-03 07:45"))
        executeClockInWitArgs(arrayOf<String>("-tcow",      "--datetime=2022-11-03 08:45"))
        executeClockInWitArgs(arrayOf<String>("-tEPP-007",  "--datetime=2022-11-03 09:15"))

        // NOV-04
        executeClockInWitArgs(arrayOf<String>("-tcow",      "--datetime=2022-11-04 08:30"))
        executeClockInWitArgs(arrayOf<String>("-tEDF-0815", "--datetime=2022-11-04 09:30"))
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-11-04 12:00"))
        executeClockInWitArgs(arrayOf<String>("-tEDF-0815", "--datetime=2022-11-04 13:00"))
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-11-04 17:30"))
        executeClockInWitArgs(arrayOf<String>("-tEDF-0815", "--datetime=2022-11-04 20:00"))

        // NOV-08
        executeClockInWitArgs(arrayOf<String>("-tEPP-007",  "--datetime=2022-11-08 08:30"))
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-11-08 11:55"))
        executeClockInWitArgs(arrayOf<String>("-tEPP-0815", "--datetime=2022-11-08 12:45"))
        executeClockInWitArgs(arrayOf<String>("-tallhands", "--datetime=2022-11-08 15:20"))
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-11-08 16:50"))

        // NOV-07 ... insert after NOV-08
        executeClockInWitArgs(arrayOf<String>("-tEDF-0815", "--datetime=2022-11-07 09:30"))
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-11-07 11:00"))

        // NOV-09
        executeClockInWitArgs(arrayOf<String>("-tcow",      "--datetime=2022-11-09 09:00"))
        executeClockInWitArgs(arrayOf<String>("-tEPP-123",  "--datetime=2022-11-09 11:00"))
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-11-09 12:00"))
        executeClockInWitArgs(arrayOf<String>("-tEPP-123",  "--datetime=2022-11-09 13:00"))
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-11-09 22:00"))

        // NOV-25
        executeClockInWitArgs(arrayOf<String>("-tcow",      "--datetime=2022-11-25 09:30"))
        executeClockInWitArgs(arrayOf<String>("-tEPP-123",  "--datetime=2022-11-25 09:45"))
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-11-25 10:55"))

        // NOV-29
        executeClockInWitArgs(arrayOf<String>("-tEPP-007",  "--datetime=2022-11-29 08:30"))
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-11-29 11:58"))
        executeClockInWitArgs(arrayOf<String>("-tEPP-123",  "--datetime=2022-11-29 13:00"))
        executeClockInWitArgs(arrayOf<String>("-tjourfixe", "--datetime=2022-11-29 14:00"))
        executeClockInWitArgs(arrayOf<String>("-tEPP-123",  "--datetime=2022-11-29 15:30"))
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-11-29 18:03"))

        // NOV-30
        executeClockInWitArgs(arrayOf<String>("-tcow",      "--datetime=2022-11-30 09:30"))
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-11-30 12:00"))
        executeClockInWitArgs(arrayOf<String>("-tEPP-0815", "--datetime=2022-11-30 13:00"))
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-11-30 15:30"))
        executeClockInWitArgs(arrayOf<String>("-tEPP-0815", "--datetime=2022-11-30 17:00"))
        executeClockOutWitArgs(arrayOf<String>(             "--datetime=2022-11-30 22:45"))

        val output = tapSystemOut {
            executeMonthlySummaryWitArgs(arrayOf<String>("-m2022-11"))
        }

        splitIgnoreBlank(output) shouldBe listOf(
                "2022-11-03: No final clock-out found. Will insert one to fill up working time to 09:00 hours.",
                "2022-11-04: No final clock-out found. Will insert one to fill up working time to 09:00 hours.",
                "[SUMMARY for 2022-11]",
                "┌──────────────┬──────┬──────┬──────┬──────╦──────┬──────┬──────╦──────╦──────┬──────┐",
                "│ day of month │    1 │    2 │    3 │    4 ║    7 │    8 │    9 ║   25 ║   29 │   30 │",
                "│ weekday      │  TUE │  WED │  THU │  FRI ║  MON │  TUE │  WED ║  FRI ║  TUE │  WED │",
                "├──────────────┼──────┼──────┼──────┼──────╬──────┼──────┼──────╬──────╬──────┼──────┤",
                "│ clock-in     │ 07:30│ 07:45│ 07:45│ 08:30║ 09:30│ 08:30│ 09:00║ 09:30║ 08:30│ 09:30│",
                "│ clock-out    │ 17:30│ 17:30│ 16:45│ 21:00║ 11:00│ 16:50│ 22:00║ 10:55║ 18:03│ 22:45│",
                "├══════════════┼══════┼══════┼══════┼══════╬══════┼══════┼══════╬══════╬══════┼══════┤",
                "│ DoD          │ 01:30│      │ 00:30│ 01:00║      │      │ 02:00║ 00:15║      │ 02:30│",
                "│ ITS meetings │ 01:00│      │      │      ║      │ 01:30│      ║      ║ 01:30│      │",
                "│ ProjectA     │ 07:30│ 02:00│ 08:30│      ║      │ 03:25│      ║      ║ 03:28│      │",
                "│ ProjectB     │      │ 01:00│      │      ║      │ 02:35│ 10:00║ 01:10║ 03:33│ 08:15│",
                "│ EDF-2223     │      │ 02:00│      │      ║      │      │      ║      ║      │      │",
                "│ Recruiting   │      │ 03:30│      │      ║      │      │      ║      ║      │      │",
                "│ EDF-0815     │      │      │      │ 08:00║ 01:30│      │      ║      ║      │      │",
                "├──────────────┼──────┼──────┼──────┼──────╬──────┼──────┼──────╬──────╬──────┼──────┤",
                "│ total        │ 10:00│ 08:30│ 09:00│ 09:00║ 01:30│ 07:30│ 12:00║ 01:25║ 08:31│ 10:45│",
                "└──────────────┴──────┴──────┴──────┴──────╩──────┴──────┴──────╩──────╩──────┴──────┘")
    }

})

fun splitIgnoreBlank(output: String): List<String> {
    return output.split("\n").filter { StringUtils.isNotBlank(it) }
}

