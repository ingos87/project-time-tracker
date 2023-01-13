package its.time.tracker

import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import its.time.tracker.service.util.CompliantWorkingTime
import its.time.tracker.service.util.WorkingTimeCalculator
import its.time.tracker.service.util.WorkingTimeResult
import java.time.Duration
import java.time.LocalTime

class WorkingTimeCalculatorTests : StringSpec({

    "toCompliantWorkingTime adapts final clock-out according to actual working time plus legal breaks" {
        listOf(
            // no working
            wrkgRes("06:00", "06:00", 0, 0)     to complWrkgRes("06:00", "06:00", 0, "06:00", "06:00", 0),
            // <6h working time
            wrkgRes("08:00", "10:00", 120, 0)   to complWrkgRes("08:00", "10:00", 120, "08:00", "10:00", 120),
            wrkgRes("08:00", "15:00", 120, 300) to complWrkgRes("08:00", "15:00", 120, "08:00", "10:00", 120),
            // 6h working time
            wrkgRes("08:00", "14:00", 360, 0)   to complWrkgRes("08:00", "14:00", 360, "08:00", "14:00", 360),
            wrkgRes("08:00", "15:00", 360, 60)  to complWrkgRes("08:00", "15:00", 360, "08:00", "14:00", 360),
            // >6h working time
            wrkgRes("08:00", "14:01", 361, 0)   to complWrkgRes("08:00", "14:01", 361, "08:00", "14:31", 361),
            wrkgRes("08:00", "14:31", 361, 30)  to complWrkgRes("08:00", "14:31", 361, "08:00", "14:31", 361),
            // 9h working time
            wrkgRes("08:00", "17:00", 540, 0)   to complWrkgRes("08:00", "17:00", 540, "08:00", "17:30", 540),
            wrkgRes("08:00", "17:40", 540, 40)  to complWrkgRes("08:00", "17:40", 540, "08:00", "17:30", 540),
            // >9h working time
            wrkgRes("08:00", "17:01", 541, 0)   to complWrkgRes("08:00", "17:01", 541, "08:00", "17:46", 541),
            wrkgRes("08:00", "19:01", 541, 120) to complWrkgRes("08:00", "19:01", 541, "08:00", "17:46", 541),
            // 10h working time
            wrkgRes("08:00", "18:00", 600, 0)   to complWrkgRes("08:00", "18:00", 600, "08:00", "18:45", 600),
            wrkgRes("08:00", "19:30", 600, 90)  to complWrkgRes("08:00", "19:30", 600, "08:00", "18:45", 600),
            // >10h working time
            wrkgRes("08:00", "18:01", 601, 0)   to complWrkgRes("08:00", "18:01", 601, "08:00", "18:45", 600),
            wrkgRes("08:00", "19:15", 605, 70)  to complWrkgRes("08:00", "19:15", 605, "08:00", "18:45", 600),
        ).forAll { (workingTimeResult, compliantWorkingTimeResult) ->
            WorkingTimeCalculator().toCompliantWorkingTime(workingTimeResult) shouldBe compliantWorkingTimeResult
        }
    }

    "toCompliantWorkingTime moves clock in and out if legal working time window is not met" {
        listOf(
            // valid evening deadline
            wrkgRes("16:00", "21:00", 300, 0)   to complWrkgRes("16:00", "21:00", 300, "16:00", "21:00", 300),
            // evening deadline violated
            wrkgRes("16:00", "21:30", 330, 0)   to complWrkgRes("16:00", "21:30", 330, "15:30", "21:00", 330),
            // valid morning deadline
            wrkgRes("06:00", "11:00", 300, 0)   to complWrkgRes("06:00", "11:00", 300, "06:00", "11:00", 300),
        ).forAll { (workingTimeResult, compliantWorkingTimeResult) ->
            WorkingTimeCalculator().toCompliantWorkingTime(workingTimeResult) shouldBe compliantWorkingTimeResult
        }
    }
})

fun wrkgRes(cIn: String, cOut: String, wT: Int, bT: Int): WorkingTimeResult {
    return WorkingTimeResult(
        firstClockIn = LocalTime.parse(cIn),
        lastClockOut = LocalTime.parse(cOut),
        totalWorkingTime = Duration.ofMinutes(wT.toLong()),
        totalBreakTime = Duration.ofMinutes(bT.toLong()),
    )
}

fun complWrkgRes(cIn: String, cOut: String, wT: Int, ccIn: String, ccOut: String, cWT: Int): CompliantWorkingTime {
    return CompliantWorkingTime(
        originalClockIn = LocalTime.parse(cIn),
        originalClockOut = LocalTime.parse(cOut),
        originalTotalWorkingTime = Duration.ofMinutes(wT.toLong()),
        compliantClockIn = LocalTime.parse(ccIn),
        compliantClockOut = LocalTime.parse(ccOut),
        compliantTotalWorkingTime = Duration.ofMinutes(cWT.toLong()),
    )
}