package its.time.tracker

import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import its.time.tracker.service.util.WorkDaySummary
import java.time.Duration
import java.time.LocalTime

class WorkDaySummaryTests : StringSpec({

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
            // flex day
            WorkDaySummary(null, null, Duration.ZERO, Duration.ZERO) to Pair(WorkDaySummary(null, null, Duration.ZERO, Duration.ZERO), Duration.ZERO),
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
})

fun wrkDay(cIn: String, cOut: String, wT: String, bT: String): WorkDaySummary {
    return WorkDaySummary(
        clockIn = LocalTime.parse(cIn),
        clockOut = LocalTime.parse(cOut),
        workDuration = Duration.parse(wT),
        breakDuration = Duration.parse(bT),
    )
}