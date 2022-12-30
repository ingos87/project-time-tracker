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
                "=== SUMMARY for 2022-01-03 ===\n" +
                "+---------------------------------------+\n" +
                "| clock-in:  07:30                      |\n" +
                "| clock-out: 16:30                      |\n" +
                "|_________________                      |\n" +
                "| total work time:  09:00               |\n" +
                "| total break time: 00:00               |\n" +
                "+=======================================+\n" +
                "| ProjectA: 09:00  (EPP-007)\n" +
                "+---------------------------------------+\n"
    }

    test("day with breaks and several projects") {
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
                "=== SUMMARY for 2022-01-03 ===\n" +
                "+---------------------------------------+\n" +
                "| clock-in:  07:30                      |\n" +
                "| clock-out: 20:52                      |\n" +
                "|_________________                      |\n" +
                "| total work time:  09:42               |\n" +
                "| total break time: 03:40               |\n" +
                "+=======================================+\n" +
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
                "=== SUMMARY for 2022-01-03 ===\n" +
                "+---------------------------------------+\n" +
                "| clock-in:  07:30                      |\n" +
                "| clock-out: 18:00                      |\n" +
                "|_________________                      |\n" +
                "| total work time:  09:30               |\n" +
                "| total break time: 01:00               |\n" +
                "+=======================================+\n" +
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
                "=== SUMMARY for 2022-01-03 ===\n" +
                "+---------------------------------------+\n" +
                "| clock-in:  09:30                      |\n" +
                "| clock-out: 19:30                      |\n" +
                "|_________________                      |\n" +
                "| total work time:  09:00               |\n" +
                "| total break time: 01:00               |\n" +
                "+=======================================+\n" +
                "| ProjectA: 09:00  (EPP-007)\n" +
                "+---------------------------------------+\n"
    }

    xtest("monthly summary for one day") {
        // TODO write clock-ins and outs

        val output = tapSystemOut {
            executeMonthlySummaryWitArgs(arrayOf<String>("-d2022-11"))
        }

        output shouldBe
                "=== SUMMARAY for 2022-11 ===\n" +
                "+--------------+------+\n" +
                "| day of month |    2 |\n" +
                "| weekday      |  WED |\n" +
                "+--------------+------+\n" +
                "| clock-in     | 07:45|\n" +
                "| clock-out    | 17:30|\n" +
                "+--------------+------+\n" +
                "| projectA     | 09:00|\n" +
                "| projectB     | 02:00|\n" +
                "| DoD          | 02:00|\n" +
                "| ITS meetings | 02:00|\n" +
                "| EDF-0815     | 02:00|\n" +
                "| EDF-2223     | 02:00|\n" +
                "| Recruiting   | 02:00|\n" +
                "+--------------+------+\n" +
                "| total        | 11:00|\n"
    }

    xtest("monthly summary") {
        // TODO write clock-ins and outs

        val output = tapSystemOut {
            executeMonthlySummaryWitArgs(arrayOf<String>("-d2022-11"))
        }

        output shouldBe
                "=== SUMMARAY for 2022-11 ===\n" +
                "+--------------+------+------+------+------++------+------+------++------++------+------+\n" +
                "| day of month |    1 |    2 |    3 |    4 ||    7 |    8 |    9 ||   25 ||   29 |   30 |\n" +
                "| weekday      |  TUE |  WED |  THU |  FRI ||  MON |  TUE |  WED ||  FRI ||  TUE |  WED |\n" +
                "+--------------+------+------+------+------++------+------+------++------++------+------+\n" +
                "| clock-in     | 07:30| 07:45| 07:45| 08:30|| 09:30| 08:30| 09:00|| 09:30|| 08:30| 09:00|\n" +
                "| clock-out    | 17:30| 17:30| 17:30| 17:30|| 11:00| 15:30| 22:00|| 11:00|| 15:30| 22:00|\n" +
                "+--------------+------+------+------+------++------+------+------++------++------+------+\n" +
                "| projectA     | 10:30| 09:00| 02:00|      ||      | 03:33|      ||      || 03:33|      |\n" +
                "| projectB     |      | 02:00| 02:00|      ||      | 03:33|      ||      || 03:33|      |\n" +
                "| DoD          |      | 02:00| 02:00| 04:56||      |      |      ||      ||      |      |\n" +
                "| ITS meetings |      | 02:00| 02:00| 04:56||      |      |      ||      ||      |      |\n" +
                "| EDF-0815     |      | 02:00| 02:00| 04:56||      |      |      ||      ||      |      |\n" +
                "| EDF-2223     |      | 02:00| 02:00| 04:56||      |      |      ||      ||      |      |\n" +
                "| Recruiting   |      | 02:00| 02:00| 04:56||      |      |      ||      ||      |      |\n" +
                "+--------------+------+------+------+------++------+------+------++------++------+------+\n" +
                "| total        | 10:30| 11:00| 06:00| 04:56||      | 07:06|      ||      || 07:06|      |\n"
    }
})

