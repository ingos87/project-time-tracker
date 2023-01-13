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

    // TODO add more testcases and finish implementation
    "toValidDate returns same string for valid date" {
        listOf(
            // no working
            wrkgRes("00:00", "00:00", 0, 0)    to complWrkgRes("00:00", "00:00", 0, "00:00", "00:00", 0),
            // <6h working time
            wrkgRes("08:00", "10:00", 120, 0)  to complWrkgRes("08:00", "10:00", 120, "08:00", "10:00", 120),
            // 6h working time
            wrkgRes("08:00", "14:00", 360, 0)  to complWrkgRes("08:00", "14:00", 360, "08:00", "14:00", 360),
            // >6h working time
            wrkgRes("08:00", "14:01", 361, 0)  to complWrkgRes("08:00", "14:01", 361, "08:00", "14:31", 361),
            // 9h working time
            //wrkgRes("08:00", "17:00", 480, 60) to complWrkgRes("08:00", "17:00", 480, "08:00", "16:30", 480),
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
        workingTimeDiff = Duration.ZERO,
    )
}