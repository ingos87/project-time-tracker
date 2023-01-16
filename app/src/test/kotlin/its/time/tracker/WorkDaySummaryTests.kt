package its.time.tracker

import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import its.time.tracker.service.util.WorkDaySummary
import java.time.Duration
import java.time.LocalTime

class WorkDaySummaryTests : StringSpec({

    "getTotalBreakDuration works ..." {
        listOf(
            Duration.parse("PT1H") to Duration.ofMinutes(0),
            Duration.parse("PT5H59M") to Duration.ofMinutes(0),
            Duration.parse("PT6H") to Duration.ofMinutes(0),
            Duration.parse("PT6H1M") to Duration.ofMinutes(30),
            Duration.parse("PT8H59M") to Duration.ofMinutes(30),
            Duration.parse("PT9H") to Duration.ofMinutes(30),
            Duration.parse("PT9H1M") to Duration.ofMinutes(45),
            Duration.parse("PT10H") to Duration.ofMinutes(45),
        ).forAll { (workingTime, totalBreakTime) ->
            WorkDaySummary.getTotalBreakDuration(workingTime) shouldBe totalBreakTime
        }
    }

    "addWorkingTime changes clock in/out and working time according to legal max working time" {
        listOf(
            Pair(wrkDay("08:00", "19:45", "PT11H", "PT45M"), Duration.ZERO)                  to Pair(wrkDay("08:00", "18:45", "PT10H", "PT45M"), Duration.ofHours(1)),
            Pair(wrkDay("08:00", "19:45", "PT11H", "PT45M"), Duration.ofHours(1))      to Pair(wrkDay("08:00", "18:45", "PT10H", "PT45M"), Duration.ofHours(2)),
            Pair(wrkDay("08:00", "12:00", "PT4H", "PT0H"), Duration.ZERO)                    to Pair(wrkDay("08:00", "12:00", "PT4H", "PT0H"), Duration.ZERO),
            Pair(wrkDay("08:00", "12:00", "PT4H", "PT0H"), Duration.ofHours(1))        to Pair(wrkDay("08:00", "13:00", "PT5H", "PT0H"), Duration.ZERO),
            Pair(wrkDay("08:00", "13:00", "PT5H", "PT0H"), Duration.ofHours(2))        to Pair(wrkDay("08:00", "15:30", "PT7H", "PT30M"), Duration.ZERO),
            Pair(wrkDay("08:00", "12:00", "PT4H", "PT0H"), Duration.ofHours(6))        to Pair(wrkDay("08:00", "18:45", "PT10H", "PT45M"), Duration.ZERO),
            Pair(wrkDay("08:00", "15:00", "PT7H", "PT30M"), Duration.ofMinutes(150)) to Pair(wrkDay("08:00", "17:45", "PT9H30M", "PT45M"), Duration.ZERO),
            Pair(wrkDay("08:00", "18:45", "PT10H", "PT45M"), Duration.ofHours(1))      to Pair(wrkDay("08:00", "18:45", "PT10H", "PT45M"), Duration.ofHours(1)),
            Pair(wrkDay("08:00", "12:00", "PT4H", "PT0H"), Duration.ofHours(8))        to Pair(wrkDay("08:00", "18:45", "PT10H", "PT45M"), Duration.ofHours(2)),
        ).forAll { (input, expectedResult) ->
            input.first.addWorkingTime(input.second) shouldBe expectedResult
        }
    }

    "makeCompliant adapts final clock-out according to actual working time plus legal breaks" {
        listOf(
            // no working time
            wrkDay("06:00", "06:00", "PT0H", "PT0H")       to Pair(wrkDay( "06:00", "06:00", "PT0H", "PT0H"), Duration.ZERO),
            // <6h working time
            wrkDay("08:00", "10:00", "PT2H", "PT0H")       to Pair(wrkDay("08:00", "10:00", "PT2H", "PT0H"), Duration.ZERO),
            wrkDay("08:00", "15:00", "PT2H", "PT5H")       to Pair(wrkDay("08:00", "10:00", "PT2H", "PT0H"), Duration.ZERO),
            // 6h working time
            wrkDay("08:00", "14:00", "PT6H", "PT0H")       to Pair(wrkDay("08:00", "14:00", "PT6H", "PT0H"), Duration.ZERO),
            wrkDay("08:00", "15:00", "PT6H", "PT1H")       to Pair(wrkDay("08:00", "14:00", "PT6H", "PT0H"), Duration.ZERO),
            // >6h working time
            wrkDay("08:00", "14:01", "PT6H1M", "PT0H")     to Pair(wrkDay("08:00", "14:31", "PT6H1M", "PT30M"), Duration.ZERO),
            wrkDay("08:00", "14:31", "PT6H1M", "PT30M")    to Pair(wrkDay("08:00", "14:31", "PT6H1M", "PT30M"), Duration.ZERO),
            // 9h working time
            wrkDay("08:00", "17:00", "PT9H", "PT0H")       to Pair(wrkDay("08:00", "17:30", "PT9H", "PT30M"), Duration.ZERO),
            wrkDay("08:00", "17:40", "PT9H", "PT40M")      to Pair(wrkDay("08:00", "17:30", "PT9H", "PT30M"), Duration.ZERO),
            // >9h working time
            wrkDay("08:00", "17:01", "PT9H1M", "PT0H")     to Pair(wrkDay("08:00", "17:46", "PT9H1M", "PT45M"), Duration.ZERO),
            wrkDay("08:00", "19:01", "PT9H1M", "PT2H")     to Pair(wrkDay("08:00", "17:46", "PT9H1M", "PT45M"), Duration.ZERO),
            // 10h working time
            wrkDay("08:00", "18:00", "PT10H", "PT0H")      to Pair(wrkDay("08:00", "18:45", "PT10H", "PT45M"), Duration.ZERO),
            wrkDay("08:00", "19:30", "PT10H", "PT1H30M")   to Pair(wrkDay("08:00", "18:45", "PT10H", "PT45M"), Duration.ZERO),
            // >10h working time
            wrkDay("08:00", "18:01", "PT10H1M", "PT0H")    to Pair(wrkDay("08:00", "18:45", "PT10H", "PT45M"), Duration.ofMinutes(1)),
            wrkDay("08:00", "19:15", "PT10H5M", "PT1H10M") to Pair(wrkDay("08:00", "18:45", "PT10H", "PT45M"), Duration.ofMinutes(5)),
        ).forAll { (workDaySummary, expectedResult) ->
            workDaySummary.makeCompliant() shouldBe expectedResult
        }
    }

    "makeCompliant moves clock in and out if legal working time window is not met" {
        listOf(
            // valid evening deadline
            wrkDay("16:00", "21:00", "PT5H", "PT0H")       to Pair(wrkDay("16:00", "21:00", "PT5H", "PT0H"), Duration.ZERO),
            // evening deadline violated
            wrkDay("16:00", "21:30", "PT5H30M", "PT0H")    to Pair(wrkDay("15:30", "21:00", "PT5H30M", "PT0H"), Duration.ZERO),
            // valid morning deadline
            wrkDay("06:00", "11:00", "PT5H", "PT0H")       to Pair(wrkDay("06:00", "11:00", "PT5H", "PT0H"), Duration.ZERO),
            // morning deadline violated
            wrkDay("05:45", "10:20", "PT4H35M", "PT0H")    to Pair(wrkDay("06:00", "10:35", "PT4H35M", "PT0H"), Duration.ZERO),
            // both violated
            wrkDay("05:30", "21:30", "PT5H30M", "PT6H30M") to Pair(wrkDay("06:00", "11:30", "PT5H30M", "PT0H"), Duration.ZERO),
        ).forAll { (workDaySummary, expectedResult) ->
            workDaySummary.makeCompliant() shouldBe expectedResult
        }
    }

    "makeCompliant changes clock in/out and working time all according to legal parameters" {
        listOf(
            wrkDay("11:00", "22:00", "PT11H", "PT0H")    to Pair(wrkDay("10:15", "21:00", "PT10H", "PT45M"), Duration.ofHours(1)),
            wrkDay("04:30", "15:00", "PT10H30M", "PT0H") to Pair(wrkDay("06:00", "16:45", "PT10H", "PT45M"), Duration.ofMinutes(30)),
            wrkDay("04:30", "22:00", "PT17H30M", "PT0H") to Pair(wrkDay("06:00", "16:45", "PT10H", "PT45M"), Duration.parse("PT7H30M")),
        ).forAll { (workDaySummary, expectedResult) ->
            workDaySummary.makeCompliant() shouldBe expectedResult
        }
    }

    "postpone moves work clockIn and clockOut correctly" {
        listOf(
            Pair(wrkDay("15:00", "22:00", "PT7H", "PT30H"), Duration.ZERO)              to Pair(wrkDay("15:00", "22:00", "PT7H", "PT30H"), Duration.ZERO),
            Pair(wrkDay("15:00", "21:00", "PT6H", "PT0H"), Duration.ZERO)               to Pair(wrkDay("15:00", "21:00", "PT6H", "PT0H"), Duration.ZERO),
            Pair(wrkDay("15:00", "21:00", "PT6H", "PT0H"), Duration.ofHours(1))   to Pair(wrkDay("15:00", "21:00", "PT6H", "PT0M"), Duration.ofHours(1)),
            Pair(wrkDay("15:00", "19:00", "PT4H", "PT0H"), Duration.ofHours(6))   to Pair(wrkDay("17:00", "21:00", "PT4H", "PT0M"), Duration.ofHours(4)),
            Pair(wrkDay("15:00", "22:00", "PT7H", "PT0H"), Duration.ofHours(1))   to Pair(wrkDay("14:00", "21:00", "PT7H", "PT0M"), Duration.ofHours(2)),
        ).forAll { (input, expectedResult) ->
            input.first.postpone(input.second) shouldBe expectedResult
        }
    }

    "prepone moves work clockIn and clockOut correctly" {
        listOf(
            Pair(wrkDay("15:00", "22:00", "PT7H", "PT30H"), Duration.ZERO)             to Pair(wrkDay("15:00", "22:00", "PT7H", "PT30H"), Duration.ZERO),
            Pair(wrkDay("15:00", "21:00", "PT6H", "PT0H"), Duration.ZERO)              to Pair(wrkDay("15:00", "21:00", "PT6H", "PT0H"), Duration.ZERO),
            Pair(wrkDay("12:00", "22:00", "PT10H", "PT0H"), Duration.ofHours(1)) to Pair(wrkDay("11:00", "21:00", "PT10H", "PT0M"), Duration.ZERO),
            Pair(wrkDay("06:00", "09:00", "PT3H", "PT0H"), Duration.ofHours(1))  to Pair(wrkDay("06:00", "09:00", "PT3H", "PT0H"), Duration.ofHours(1)),
            Pair(wrkDay("07:00", "11:00", "PT4H", "PT0H"), Duration.ofHours(3))  to Pair(wrkDay("06:00", "10:00", "PT4H", "PT0H"), Duration.ofHours(2)),
            Pair(wrkDay("05:00", "11:00", "PT6H", "PT0H"), Duration.ofHours(1))  to Pair(wrkDay("06:00", "12:00", "PT6H", "PT0H"), Duration.ofHours(2)),
        ).forAll { (input, expectedResult) ->
            input.first.prepone(input.second) shouldBe expectedResult
        }
    }
})

fun wrkDay(cIn: String, cOut: String, wT: String, bT: String): WorkDaySummary {
    return WorkDaySummary(
        clockIn = LocalTime.parse(cIn),
        clockOut = LocalTime.parse(cOut),
        workDuration = Duration.parse(wT),
        breakDuration = Duration.parse(bT),
    )
}