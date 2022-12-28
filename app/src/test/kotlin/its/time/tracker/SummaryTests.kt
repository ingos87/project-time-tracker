package its.time.tracker

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SummaryTests : FunSpec({

    beforeEach {
        ensureCsvEmpty()
    }

    test("simple clock-in and clock-out") {
        executeClockInWitArgs(arrayOf<String>("-tEPP-007", "--datetime=20220103_0730"))
        executeClockOutWitArgs(arrayOf<String>("--datetime=20220103_1630"))

        val output = tapSystemOut {
            executeDailySummaryWitArgs(arrayOf<String>("-d20220103"))
        }

        output shouldBe
                "+-------------------------------------+\n" +
                "| work hours summary for day 20220103 |\n" +
                "| clock-in:  0730                     |\n" +
                "| clock-out: 1630                     |\n" +
                "|_____________                        |\n" +
                "| total work time:  0900              |\n" +
                "| total break time: 0000              |\n" +
                "+-------------------------------------+\n" +
                "+-------------------------------------+\n" +
                "| project summary for day 20220103\n" +
                "| ProjectA: 0900  (EPP-007)\n" +
                "+-------------------------------------+\n"
    }

    test("day with breaksand several projects") {
        executeClockInWitArgs(arrayOf<String>("-tEPP-007", "--datetime=20220103_0730"))
        executeClockInWitArgs(arrayOf<String>("-tEPP-008", "--datetime=20220103_0900")) // worktime 1:30
        executeClockOutWitArgs(arrayOf<String>("--datetime=20220103_1125")) // worktime 3:55
        executeClockInWitArgs(arrayOf<String>("-tEPP-123", "--datetime=20220103_1330")) // break 2:05
        executeClockOutWitArgs(arrayOf<String>("--datetime=20220103_1530")) // worktime 2:00
        executeClockInWitArgs(arrayOf<String>("-tallhands", "--datetime=20220103_1705")) // break 1:35
        executeClockInWitArgs(arrayOf<String>("-tEDF-99", "--datetime=20220103_1805")) // worktime 1:00
        executeClockOutWitArgs(arrayOf<String>("--datetime=20220103_2052")) // worktime 2:47

        val output = tapSystemOut {
            executeDailySummaryWitArgs(arrayOf<String>("-d20220103"))
        }

        output shouldBe
                "+-------------------------------------+\n" +
                "| work hours summary for day 20220103 |\n" +
                "| clock-in:  0730                     |\n" +
                "| clock-out: 2052                     |\n" +
                "|_____________                        |\n" +
                "| total work time:  0942              |\n" +
                "| total break time: 0340              |\n" +
                "+-------------------------------------+\n" +
                "+-------------------------------------+\n" +
                "| project summary for day 20220103\n" +
                "| ProjectA: 0355  (EPP-007,EPP-008)\n" +
                "| ProjectB: 0200  (EPP-123)\n" +
                "| ITS meetings: 0100  (allhands)\n" +
                "| EDF-99: 0247  (EDF-99)\n" +
                "+-------------------------------------+\n"
    }
})

