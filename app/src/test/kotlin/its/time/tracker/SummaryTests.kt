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
                "| summary for day 20220103            |\n" +
                "| clock-in  0730                      |\n" +
                "| clock-out 1630                      |\n" +
                "______________\n" +
                "| total work time  0900               |\n" +
                "| total break time 0000               |\n" +
                "+-------------------------------------+\n"
    }

    test("day with breaks") {
        executeClockInWitArgs(arrayOf<String>("-tEPP-007", "--datetime=20220103_0730"))
        executeClockOutWitArgs(arrayOf<String>("--datetime=20220103_1130")) // worktime 4:00
        executeClockInWitArgs(arrayOf<String>("-tEPP-008", "--datetime=20220103_1330")) // break 2:00
        executeClockOutWitArgs(arrayOf<String>("--datetime=20220103_1530")) // worktime 2:00
        executeClockInWitArgs(arrayOf<String>("-tEPP-008", "--datetime=20220103_1705")) // break 1:35
        executeClockOutWitArgs(arrayOf<String>("--datetime=20220103_2055")) // worktime 3:50

        val output = tapSystemOut {
            executeDailySummaryWitArgs(arrayOf<String>("-d20220103"))
        }

        output shouldBe
                "+-------------------------------------+\n" +
                "| summary for day 20220103            |\n" +
                "| clock-in  0730                      |\n" +
                "| clock-out 2055                      |\n" +
                "______________\n" +
                "| total work time  0950               |\n" +
                "| total break time 0335               |\n" +
                "+-------------------------------------+\n"
    }
})

