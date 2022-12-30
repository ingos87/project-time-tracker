package its.time.tracker

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SummaryTests : FunSpec({

    beforeEach {
        ensureCsvEmpty()
    }

    test("simple clock-in and clock-out") {
        executeClockInWitArgs(arrayOf<String>("-tEPP-007", "--datetime=2022-01-03 07:30"))
        executeClockOutWitArgs(arrayOf<String>("--datetime=2022-01-03 16:30"))

        val output = tapSystemOut {
            executeDailySummaryWitArgs(arrayOf<String>("-d2022-01-03"))
        }

        output shouldBe
                "+---------------------------------------+\n" +
                "| work hours summary for day 2022-01-03 |\n" +
                "| clock-in:  07:30                      |\n" +
                "| clock-out: 16:30                      |\n" +
                "|_____________                          |\n" +
                "| total work time:  09:00               |\n" +
                "| total break time: 00:00               |\n" +
                "+---------------------------------------+\n" +
                "+---------------------------------------+\n" +
                "| project summary for day 2022-01-03\n" +
                "| ProjectA: 09:00  (EPP-007)\n" +
                "+---------------------------------------+\n"
    }

    test("day with breaksand several projects") {
        executeClockInWitArgs(arrayOf<String>("-tEPP-007", "--datetime=2022-01-03 07:30"))
        executeClockInWitArgs(arrayOf<String>("-tEPP-008", "--datetime=2022-01-03 09:00")) // worktime 1:30
        executeClockOutWitArgs(arrayOf<String>("--datetime=2022-01-03 11:25")) // worktime 3:55
        executeClockInWitArgs(arrayOf<String>("-tEPP-123", "--datetime=2022-01-03 13:30")) // break 2:05
        executeClockOutWitArgs(arrayOf<String>("--datetime=2022-01-03 15:30")) // worktime 2:00
        executeClockInWitArgs(arrayOf<String>("-tallhands", "--datetime=2022-01-03 17:05")) // break 1:35
        executeClockInWitArgs(arrayOf<String>("-tEDF-99", "--datetime=2022-01-03 18:05")) // worktime 1:00
        executeClockOutWitArgs(arrayOf<String>("--datetime=2022-01-03 20:52")) // worktime 2:47

        val output = tapSystemOut {
            executeDailySummaryWitArgs(arrayOf<String>("-d2022-01-03"))
        }

        output shouldBe
                "+---------------------------------------+\n" +
                "| work hours summary for day 2022-01-03 |\n" +
                "| clock-in:  07:30                      |\n" +
                "| clock-out: 20:52                      |\n" +
                "|_____________                          |\n" +
                "| total work time:  09:42               |\n" +
                "| total break time: 03:40               |\n" +
                "+---------------------------------------+\n" +
                "+---------------------------------------+\n" +
                "| project summary for day 2022-01-03\n" +
                "| ProjectA: 03:55  (EPP-007,EPP-008)\n" +
                "| ProjectB: 02:00  (EPP-123)\n" +
                "| ITS meetings: 01:00  (allhands)\n" +
                "| EDF-99: 02:47  (EDF-99)\n" +
                "+---------------------------------------+\n"
    }

    test("missing clock-out is set beyond max working hours per day") {
        executeClockInWitArgs(arrayOf<String>("-tEPP-007", "--datetime=2022-01-03 07:30"))
        executeClockOutWitArgs(arrayOf<String>("--datetime=2022-01-03 16:30"))
        executeClockInWitArgs(arrayOf<String>("-tEPP-007", "--datetime=2022-01-03 17:30"))

        val output = tapSystemOut {
            executeDailySummaryWitArgs(arrayOf<String>("-d2022-01-03"))
        }

        output shouldBe
                "No final clock-out found. Will insert one to fill up working time to 09:30 hours.\n" +
                "+---------------------------------------+\n" +
                "| work hours summary for day 2022-01-03 |\n" +
                "| clock-in:  07:30                      |\n" +
                "| clock-out: 18:00                      |\n" +
                "|_____________                          |\n" +
                "| total work time:  09:30               |\n" +
                "| total break time: 01:00               |\n" +
                "+---------------------------------------+\n" +
                "+---------------------------------------+\n" +
                "| project summary for day 2022-01-03\n" +
                "| ProjectA: 09:30  (EPP-007)\n" +
                "+---------------------------------------+\n"
    }

    test("missing clock-out is set to max working hours per day") {
        executeClockInWitArgs(arrayOf<String>("-tEPP-007", "--datetime=2022-01-03 09:30"))
        executeClockOutWitArgs(arrayOf<String>("--datetime=2022-01-03 16:30"))
        executeClockInWitArgs(arrayOf<String>("-tEPP-007", "--datetime=2022-01-03 17:30"))

        val output = tapSystemOut {
            executeDailySummaryWitArgs(arrayOf<String>("-d2022-01-03"))
        }

        output shouldBe
                "No final clock-out found. Will insert one to fill up working time to 09:00 hours.\n" +
                "+---------------------------------------+\n" +
                "| work hours summary for day 2022-01-03 |\n" +
                "| clock-in:  09:30                      |\n" +
                "| clock-out: 19:30                      |\n" +
                "|_____________                          |\n" +
                "| total work time:  09:00               |\n" +
                "| total break time: 01:00               |\n" +
                "+---------------------------------------+\n" +
                "+---------------------------------------+\n" +
                "| project summary for day 2022-01-03\n" +
                "| ProjectA: 09:00  (EPP-007)\n" +
                "+---------------------------------------+\n"
    }
})

