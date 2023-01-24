package its.time.tracker.service

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

class SummaryTests : FunSpec({

    beforeEach {
        ensureTestConfig()
        ensureCsvEmpty()
    }

    test("summary is not possible if there is no config file") {
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2022-01-03 07:30"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-01-03 16:30"))

        ensureNoConfig()

        val output = tapSystemOut {
            main(arrayOf("daily-summary", "--date=2023-01-03"))
        }

        output shouldStartWith "No config file found in ./app.json\n" +
                "Use 'java -jar app.jar init' with the according parameters"

        val output2 = tapSystemOut {
            main(arrayOf("monthly-summary", "--month=2023-01"))
        }

        output2 shouldStartWith "No config file found in ./app.json\n" +
                "Use 'java -jar app.jar init' with the according parameters"
    }

    test("today's summary shows err message if there are not clock-in events") {
        executeClockOutWitArgs(arrayOf(             "--datetime=2023-01-02 14:30"))

        val output = tapSystemOut {
            executeDailySummaryWitArgs(arrayOf("-d2023-01-02"))
        }

        splitIgnoreBlank(output) shouldBe listOf(
            "[NO SUMMARY for 2023-01-02 because there are no clock-in events]")
    }

    test("simple clock-in and clock-out") {
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2022-01-03 07:30"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-01-03 16:30"))

        val output = tapSystemOut {
            executeDailySummaryWitArgs(arrayOf("-d2022-01-03"))
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

    test("working day ends on following day") {
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2022-01-03 16:30"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-01-03 20:30"))
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2022-01-03 23:00"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-01-04 01:30"))

        val output = tapSystemOut {
            executeDailySummaryWitArgs(arrayOf("-d2022-01-03"))
        }

        splitIgnoreBlank(output) shouldBe listOf(
            "[SUMMARY for 2022-01-03]",
            "┌────────────────────────────────────────────────┐",
            "│ clock-in:         16:30                        │",
            "│ clock-out:        01:30 (2022-01-04)           │",
            "├────────────────────────────────────────────────┤",
            "│ total work time:  06:30                        │",
            "│ total break time: 02:30                        │",
            "├════════════════════════════════════════════════┤",
            "│ ProjectA:     06:30  (EPP-007)                 │",
            "└────────────────────────────────────────────────┘")
    }

    test("today's summary if working day is in progress") {
        val today = LocalDate.now()
        val todayString = DateTimeUtil.temporalToString(today, DATE_PATTERN)
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=$todayString 01:30"))
        executeClockInWitArgs(arrayOf("-tEPP-123",  "--datetime=$todayString 03:45"))
        executeClockOutWitArgs(arrayOf(             "--datetime=$todayString 04:30"))
        executeClockInWitArgs(arrayOf("-tEPP-123",  "--datetime=$todayString 04:45"))
        executeClockInWitArgs(arrayOf("-tEDF-777",  "--datetime=$todayString 05:55"))

        val startOfWorkDay = LocalDateTime.parse("${todayString}T01:30")
        val now = LocalDateTime.now()
        val currentWorkDuration = Duration.ofMinutes(MINUTES.between(startOfWorkDay, now) - 15)
        val currentEdfDuration = currentWorkDuration.minus(Duration.ofMinutes(4*60 + 10))
        val output = tapSystemOut {
            executeDailySummaryWitArgs(emptyArray())
        }

        splitIgnoreBlank(output) shouldBe listOf(
            "[today's work in progress]",
            "┌────────────────────────────────────────────────┐",
            "│ clock-in:           01:30                      │",
            "├────────────────────────────────────────────────┤",
            "│ current work time:  ${DateTimeUtil.durationToString(currentWorkDuration)}                      │",
            "│ current break time: 00:15                      │",
            "│ current work topic: EDF-777                    │",
            "├════════════════════════════════════════════════┤",
            "│ ProjectA:     02:15  (EPP-007)                 │",
            "│ ProjectB:     01:55  (EPP-123)                 │",
            "│ EDF-777:      ${DateTimeUtil.durationToString(currentEdfDuration)}  (EDF-777)                 │",
            "└────────────────────────────────────────────────┘")
    }

    test("today's summary if working day is ended") {
        val today = LocalDate.now()
        val todayString = DateTimeUtil.temporalToString(today, DATE_PATTERN)
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=$todayString 01:30"))
        executeClockInWitArgs(arrayOf("-tEPP-123",  "--datetime=$todayString 03:45"))
        executeClockOutWitArgs(arrayOf(             "--datetime=$todayString 04:30"))

        val output = tapSystemOut {
            executeDailySummaryWitArgs(emptyArray())
        }

        splitIgnoreBlank(output) shouldBe listOf(
            "[SUMMARY for $todayString]",
            "┌────────────────────────────────────────────────┐",
            "│ clock-in:         01:30                        │",
            "│ clock-out:        04:30                        │",
            "├────────────────────────────────────────────────┤",
            "│ total work time:  03:00                        │",
            "│ total break time: 00:00                        │",
            "├════════════════════════════════════════════════┤",
            "│ ProjectA:     02:15  (EPP-007)                 │",
            "│ ProjectB:     00:45  (EPP-123)                 │",
            "└────────────────────────────────────────────────┘")
    }

    test("day with breaks and several projects") {
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2022-01-03 07:30"))
        executeClockInWitArgs(arrayOf("-tEPP-008",  "--datetime=2022-01-03 09:00")) // workingtime 1:30
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-01-03 11:25")) // workingtime 3:55
        executeClockInWitArgs(arrayOf("-tEPP-123",  "--datetime=2022-01-03 13:30")) // break 2:05
        executeClockInWitArgs(arrayOf("-tEPP-009",  "--datetime=2022-01-03 13:40")) // workingtime 0:10
        executeClockInWitArgs(arrayOf("-tEPP-0815", "--datetime=2022-01-03 13:50")) // workingtime 0:10
        executeClockInWitArgs(arrayOf("-tEPP-17662","--datetime=2022-01-03 14:00")) // workingtime 0:10
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-01-03 15:30")) // workingtime 1:30
        executeClockInWitArgs(arrayOf("-tallhands", "--datetime=2022-01-03 17:05")) // break 1:35
        executeClockInWitArgs(arrayOf("-tEDF-99",   "--datetime=2022-01-03 18:05")) // workingtime 1:00
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-01-03 20:52")) // workingtime 2:47

        val output = tapSystemOut {
            executeDailySummaryWitArgs(arrayOf("-d2022-01-03"))
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
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2022-01-03 07:30"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-01-03 16:30"))
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2022-01-03 17:30"))
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2022-01-04 07:30")) // ignored

        val output = tapSystemOut {
            executeDailySummaryWitArgs(arrayOf("-d2022-01-03"))
        }

        splitIgnoreBlank(output) shouldBe listOf(
            "2022-01-03: No final clock-out found. Will insert one. Work time will be 09:30 hours.",
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
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2022-01-03 09:30"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-01-03 16:30"))
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2022-01-03 17:30"))
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2022-01-04 07:30")) // ignored

        val output = tapSystemOut {
            executeDailySummaryWitArgs(arrayOf("-d2022-01-03"))
        }

        splitIgnoreBlank(output) shouldBe listOf(
            "2022-01-03: No final clock-out found. Will insert one to fill up working time to maximum (09:00 hours).",
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

    test("month's summary shows err message if there are not clock-in events") {
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-11-02 14:30"))

        val output = tapSystemOut {
            executeMonthlySummaryWitArgs(arrayOf("-m2022-11"))
        }

        splitIgnoreBlank(output) shouldBe listOf(
            "[NO SUMMARY for 2022-NOV because there are no clock-in events]")
    }

    test("monthly summary for one day with lunch break") {
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2022-11-02 07:30"))
        executeClockInWitArgs(arrayOf("-tEPP-009",  "--datetime=2022-11-02 08:20"))
        executeClockInWitArgs(arrayOf("-tcow",      "--datetime=2022-11-02 09:45"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-11-02 12:00"))
        executeClockInWitArgs(arrayOf("-tEDF-0815", "--datetime=2022-11-02 13:15"))
        executeClockInWitArgs(arrayOf("-tEDF-1234", "--datetime=2022-11-02 13:30"))
        executeClockInWitArgs(arrayOf("-tDVR-7",    "--datetime=2022-11-02 14:00"))
        executeClockInWitArgs(arrayOf("-tf2f",      "--datetime=2022-11-02 15:00"))
        executeClockInWitArgs(arrayOf("-tDVR-7",    "--datetime=2022-11-02 15:45"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-11-02 16:45"))

        val output = tapSystemOut {
            executeMonthlySummaryWitArgs(arrayOf("-m2022-11"))
        }

        splitIgnoreBlank(output) shouldBe listOf(
            "[SUMMARY for 2022-NOV]",
            "┌──────────────┬──────┐",
            "│ weekday      │  WED │",
            "│ day of month │    2 │",
            "│ week of year │   44 │",
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
        executeClockInWitArgs(arrayOf("-tcow",      "--datetime=2022-11-01 07:30"))
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2022-11-01 09:00"))
        executeClockInWitArgs(arrayOf("-tjourfixe", "--datetime=2022-11-01 14:00"))
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2022-11-01 15:00"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-11-01 17:30"))

        // NOV-02
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2022-11-02 07:45"))
        executeClockInWitArgs(arrayOf("-tEPP-123",  "--datetime=2022-11-02 09:45"))
        executeClockInWitArgs(arrayOf("-tEDF-2223", "--datetime=2022-11-02 10:45"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-11-02 12:00"))
        executeClockInWitArgs(arrayOf("-tEDF-2223", "--datetime=2022-11-02 13:15"))
        executeClockInWitArgs(arrayOf("-tDVR-3",    "--datetime=2022-11-02 14:00"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-11-02 17:30"))

        // NOV-03
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2022-11-03 07:45"))
        executeClockInWitArgs(arrayOf("-tcow",      "--datetime=2022-11-03 08:45"))
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2022-11-03 09:15"))

        // NOV-04
        executeClockInWitArgs(arrayOf("-tcow",      "--datetime=2022-11-04 08:30"))
        executeClockInWitArgs(arrayOf("-tEDF-0815", "--datetime=2022-11-04 09:30"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-11-04 12:00"))
        executeClockInWitArgs(arrayOf("-tEDF-0815", "--datetime=2022-11-04 13:00"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-11-04 17:30"))
        executeClockInWitArgs(arrayOf("-tEDF-0815", "--datetime=2022-11-04 20:00"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-11-04 21:30"))
        executeClockInWitArgs(arrayOf("-tEDF-0815", "--datetime=2022-11-04 22:00"))

        // NOV-06 .. work on sunday
        executeClockInWitArgs(arrayOf("-tEDF-0815", "--datetime=2022-11-06 20:00"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-11-06 22:45"))

        // NOV-08
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2022-11-08 08:30"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-11-08 11:55"))
        executeClockInWitArgs(arrayOf("-tEPP-0815", "--datetime=2022-11-08 12:45"))
        executeClockInWitArgs(arrayOf("-tallhands", "--datetime=2022-11-08 15:20"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-11-08 16:50"))

        // NOV-07 ... insert after NOV-08
        executeClockInWitArgs(arrayOf("-tEDF-0815", "--datetime=2022-11-07 09:30"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-11-07 11:00"))

        // NOV-09
        executeClockInWitArgs(arrayOf("-tcow",      "--datetime=2022-11-09 09:00"))
        executeClockInWitArgs(arrayOf("-tEPP-123",  "--datetime=2022-11-09 11:00"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-11-09 12:00"))
        executeClockInWitArgs(arrayOf("-tEPP-123",  "--datetime=2022-11-09 13:00"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-11-09 22:00"))

        // NOV-25
        executeClockInWitArgs(arrayOf("-tcow",      "--datetime=2022-11-25 09:30"))
        executeClockInWitArgs(arrayOf("-tEPP-123",  "--datetime=2022-11-25 09:45"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-11-25 10:55"))

        // NOV-29
        executeClockInWitArgs(arrayOf("-tEPP-007",  "--datetime=2022-11-29 08:30"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-11-29 11:58"))
        executeClockInWitArgs(arrayOf("-tEPP-123",  "--datetime=2022-11-29 13:00"))
        executeClockInWitArgs(arrayOf("-tjourfixe", "--datetime=2022-11-29 14:00"))
        executeClockInWitArgs(arrayOf("-tEPP-123",  "--datetime=2022-11-29 15:30"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-11-29 18:03"))

        // NOV-30
        executeClockInWitArgs(arrayOf("-tcow",      "--datetime=2022-11-30 09:30"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-11-30 12:00"))
        executeClockInWitArgs(arrayOf("-tEPP-0815", "--datetime=2022-11-30 13:00"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-11-30 15:30"))
        executeClockInWitArgs(arrayOf("-tEPP-0815", "--datetime=2022-11-30 17:00"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2022-11-30 22:45"))

        val output = tapSystemOut {
            executeMonthlySummaryWitArgs(arrayOf("-m2022-11"))
        }

        splitIgnoreBlank(output) shouldBe listOf(
            "2022-11-03: No final clock-out found. Will insert one to fill up working time to maximum (09:00 hours).",
            "2022-11-04: No final clock-out found. Will insert one. Work time will be 10:00 hours.",
            "[SUMMARY for 2022-NOV]",
            "┌──────────────┬──────┬──────┬──────┬──────╦──────┬──────┬──────┬──────╦──────╦──────┬──────┐",
            "│ weekday      │  TUE │  WED │  THU │  FRI ║  SUN │  MON │  TUE │  WED ║  FRI ║  TUE │  WED │",
            "│ day of month │    1 │    2 │    3 │    4 ║    6 │    7 │    8 │    9 ║   25 ║   29 │   30 │",
            "│ week of year │   44 │   44 │   44 │   44 ║   44 │   45 │   45 │   45 ║   47 ║   48 │   48 │",
            "├──────────────┼──────┼──────┼──────┼──────╬──────┼──────┼──────┼──────╬──────╬──────┼──────┤",
            "│ clock-in     │ 07:30│ 07:45│ 07:45│ 08:30║ 20:00│ 09:30│ 08:30│ 09:00║ 09:30║ 08:30│ 09:30│",
            "│ clock-out    │ 17:30│ 17:30│ 16:45│ 22:30║ 22:45│ 11:00│ 16:50│ 22:00║ 10:55║ 18:03│ 22:45│",
            "├══════════════┼══════┼══════┼══════┼══════╬══════┼══════┼══════┼══════╬══════╬══════┼══════┤",
            "│ DoD          │ 01:30│      │ 00:30│ 01:00║      │      │      │ 02:00║ 00:15║      │ 02:30│",
            "│ ITS meetings │ 01:00│      │      │      ║      │      │ 01:30│      ║      ║ 01:30│      │",
            "│ ProjectA     │ 07:30│ 02:00│ 08:30│      ║      │      │ 03:25│      ║      ║ 03:28│      │",
            "│ ProjectB     │      │ 01:00│      │      ║      │      │ 02:35│ 10:00║ 01:10║ 03:33│ 08:15│",
            "│ EDF-2223     │      │ 02:00│      │      ║      │      │      │      ║      ║      │      │",
            "│ Recruiting   │      │ 03:30│      │      ║      │      │      │      ║      ║      │      │",
            "│ EDF-0815     │      │      │      │ 09:00║ 02:45│ 01:30│      │      ║      ║      │      │",
            "├──────────────┼──────┼──────┼──────┼──────╬──────┼──────┼──────┼──────╬──────╬──────┼──────┤",
            "│ total        │ 10:00│ 08:30│ 09:00│ 10:00║ 02:45│ 01:30│ 07:30│ 12:00║ 01:25║ 08:31│ 10:45│",
            "└──────────────┴──────┴──────┴──────┴──────╩──────┴──────┴──────┴──────╩──────╩──────┴──────┘")
    }
})

