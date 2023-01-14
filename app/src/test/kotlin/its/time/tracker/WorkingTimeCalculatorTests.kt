package its.time.tracker

import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import its.time.tracker.service.util.WorkingTimeCalculator
import its.time.tracker.service.util.WorkDaySummary
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

class WorkingTimeCalculatorTests : StringSpec({

    beforeEach {
        ensureTestConfig("MON,SAT,SUN")
    }

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
            WorkingTimeCalculator().getTotalBreakDuration(workingTime) shouldBe totalBreakTime
        }
    }

    "addWorkingTimeToClockInAndOut works ..." {
        listOf(
            fun1Params(LocalTime.parse("08:00"),
                LocalTime.parse("12:00"),
                Duration.ofHours(1)) to Triple(LocalTime.parse("08:00"), LocalTime.parse("13:00"), Duration.parse("PT0H")),
            fun1Params(LocalTime.parse("08:00"),
                LocalTime.parse("13:00"),
                Duration.ofHours(2)) to Triple(LocalTime.parse("08:00"), LocalTime.parse("15:30"), Duration.parse("PT30M")),
            fun1Params(LocalTime.parse("08:00"),
                LocalTime.parse("12:00"),
                Duration.ofHours(6)) to Triple(LocalTime.parse("08:00"), LocalTime.parse("18:45"), Duration.parse("PT45M")),
            fun1Params(LocalTime.parse("08:00"),
                LocalTime.parse("15:00"),
                Duration.ofMinutes(150)) to Triple(LocalTime.parse("08:00"), LocalTime.parse("17:45"), Duration.parse("PT45M")),
        ).forAll { (params, expectedCLockInAndOut) ->
            WorkingTimeCalculator().addWorkingTimeToClockInAndOut(params.clockIn, params.clockOut, params.additionalDuration) shouldBe expectedCLockInAndOut
        }
    }

    "toCompliantWorkDaySummary adapts final clock-out according to actual working time plus legal breaks" {
        listOf(
            // no working
            wrkDay("06:00", "06:00", "PT0H", "PT0H")     to wrkDay( "06:00", "06:00", "PT0H", "PT0H"),
            // <6h working time
            wrkDay("08:00", "10:00", "PT2H", "PT0H")   to wrkDay("08:00", "10:00", "PT2H", "PT0H"),
            wrkDay("08:00", "15:00", "PT2H", "PT5H") to wrkDay("08:00", "10:00", "PT2H", "PT0H"),
            // 6h working time
            wrkDay("08:00", "14:00", "PT6H", "PT0H")   to wrkDay("08:00", "14:00", "PT6H", "PT0H"),
            wrkDay("08:00", "15:00", "PT6H", "PT1H")  to wrkDay("08:00", "14:00", "PT6H", "PT0H"),
            // >6h working time
            wrkDay("08:00", "14:01", "PT6H1M", "PT0H")   to wrkDay("08:00", "14:31", "PT6H1M", "PT30M"),
            wrkDay("08:00", "14:31", "PT6H1M", "PT30M")  to wrkDay("08:00", "14:31", "PT6H1M", "PT30M"),
            // 9h working time
            wrkDay("08:00", "17:00", "PT9H", "PT0H")   to wrkDay("08:00", "17:30", "PT9H", "PT30M"),
            wrkDay("08:00", "17:40", "PT9H", "PT40M")  to wrkDay("08:00", "17:30", "PT9H", "PT30M"),
            // >9h working time
            wrkDay("08:00", "17:01", "PT9H1M", "PT0H")   to wrkDay("08:00", "17:46", "PT9H1M", "PT45M"),
            wrkDay("08:00", "19:01", "PT9H1M", "PT2H") to wrkDay("08:00", "17:46", "PT9H1M", "PT45M"),
            // 10h working time
            wrkDay("08:00", "18:00", "PT10H", "PT0H")   to wrkDay("08:00", "18:45", "PT10H", "PT45M"),
            wrkDay("08:00", "19:30", "PT10H", "PT1H30M")  to wrkDay("08:00", "18:45", "PT10H", "PT45M"),
            // >10h working time
            wrkDay("08:00", "18:01", "PT10H1M", "PT0H")   to wrkDay("08:00", "18:45", "PT10H", "PT45M"),
            wrkDay("08:00", "19:15", "PT10H5M", "PT1H10M")  to wrkDay("08:00", "18:45", "PT10H", "PT45M"),
        ).forAll { (workDaySummary, compliantworkDaySummary) ->
            WorkingTimeCalculator().toCompliantWorkDaySummary(workDaySummary) shouldBe compliantworkDaySummary
        }
    }

    "toCompliantWorkDaySummary moves clock in and out if legal working time window is not met" {
        listOf(
            // valid evening deadline
            wrkDay("16:00", "21:00", "PT5H", "PT0H")       to wrkDay("16:00", "21:00", "PT5H", "PT0H"),
            // evening deadline violated
            wrkDay("16:00", "21:30", "PT5H30M", "PT0H")    to wrkDay("15:30", "21:00", "PT5H30M", "PT0H"),
            // valid morning deadline
            wrkDay("06:00", "11:00", "PT5H", "PT0H")       to wrkDay("06:00", "11:00", "PT5H", "PT0H"),
            // morning deadline violated
            wrkDay("05:45", "10:20", "PT4H35M", "PT0H")    to wrkDay("06:00", "10:35", "PT4H35M", "PT0H"),
            // both violated
            wrkDay("05:30", "21:30", "PT5H30M", "PT6H30M") to wrkDay("06:00", "11:30", "PT5H30M", "PT0H"),
        ).forAll { (workingTimeResult, compliantWorkingTimeResult) ->
            WorkingTimeCalculator().toCompliantWorkDaySummary(workingTimeResult) shouldBe compliantWorkingTimeResult
        }
    }

    "toCompliantWorkDaySummary changes clock in/out and working time all according to legal parameters" {
        listOf(
            wrkDay("11:00", "22:00", "PT11H", "PT0H")    to wrkDay("10:15", "21:00", "PT10H", "PT45M"),
            wrkDay("04:30", "15:00", "PT10H30M", "PT0H") to wrkDay("06:00", "16:45", "PT10H", "PT45M"),
            wrkDay("04:30", "22:00", "PT17H30M", "PT0H") to wrkDay("06:00", "16:45", "PT10H", "PT45M"),
        ).forAll { (workingTimeResult, compliantWorkingTimeResult) ->
            WorkingTimeCalculator().toCompliantWorkDaySummary(workingTimeResult) shouldBe compliantWorkingTimeResult
        }
    }

    "distributeWorkingTime returns empty map" {
        val input = HashMap<LocalDate, List<WorkDaySummary>>()
        WorkingTimeCalculator().distributeWorkingTime(input) shouldBe emptyMap()
    }

    "distributeWorkingTime does not alter data if all legal parameters are met" {
        val input = HashMap<LocalDate, List<WorkDaySummary>>()
        input[LocalDate.parse("2023-01-10")] = listOf(
            wrkDay("07:30", "16:00", "PT8H", "PT30M"),
            wrkDay("07:30", "16:00", "PT8H", "PT30M"))
        input[LocalDate.parse("2023-01-11")] = listOf(
            wrkDay("07:30", "16:00", "PT8H", "PT30M"),
            wrkDay("07:30", "16:00", "PT8H", "PT30M"))
        input[LocalDate.parse("2023-01-12")] = listOf(
            wrkDay("07:30", "16:00", "PT8H", "PT30M"),
            wrkDay("07:30", "16:00", "PT8H", "PT30M"))
        input[LocalDate.parse("2023-01-13")] = listOf(
            wrkDay("07:30", "16:00", "PT8H", "PT30M"),
            wrkDay("07:30", "16:00", "PT8H", "PT30M"))

        val expectedOutput = HashMap<LocalDate, WorkDaySummary>()
        expectedOutput[LocalDate.parse("2023-01-10")] =
            wrkDay("07:30", "16:00", "PT8H", "PT30M")
        expectedOutput[LocalDate.parse("2023-01-11")] =
            wrkDay("07:30", "16:00", "PT8H", "PT30M")
        expectedOutput[LocalDate.parse("2023-01-12")] =
            wrkDay("07:30", "16:00", "PT8H", "PT30M")
        expectedOutput[LocalDate.parse("2023-01-13")] =
            wrkDay("07:30", "16:00", "PT8H", "PT30M")

        val result = WorkingTimeCalculator().distributeWorkingTime(input)
        result.forEach{ entry ->
            entry.value.last() shouldBe expectedOutput[entry.key]
        }
    }

    "distributeWorkingTime moves excess working time to following day" {
        val input = HashMap<LocalDate, List<WorkDaySummary>>()
        input[LocalDate.parse("2023-01-10")] = listOf(
            wrkDay("07:15", "18:00", "PT10H", "PT45M"),
            wrkDay("07:15", "18:00", "PT10H", "PT45M"))
        input[LocalDate.parse("2023-01-11")] = listOf(
            wrkDay("07:15", "19:00", "PT11H", "PT45M"),
            wrkDay("07:15", "18:00", "PT10H", "PT45M"))
        input[LocalDate.parse("2023-01-12")] = listOf(
            wrkDay("07:30", "15:00", "PT7H", "PT30M"),
            wrkDay("07:30", "15:00", "PT7H", "PT30M"))
        input[LocalDate.parse("2023-01-13")] = listOf(
            wrkDay("07:30", "16:00", "PT8H", "PT30M"),
            wrkDay("07:30", "16:00", "PT8H", "PT30M"))
        
        val expectedOutput = HashMap<LocalDate, WorkDaySummary>()
        expectedOutput[LocalDate.parse("2023-01-10")] =
            wrkDay("07:15", "18:00", "PT10H", "PT45M")
        expectedOutput[LocalDate.parse("2023-01-11")] =
            wrkDay("07:15", "18:00", "PT10H", "PT45M")
        expectedOutput[LocalDate.parse("2023-01-12")] =
            wrkDay("07:30", "16:00", "PT8H", "PT30M")
        expectedOutput[LocalDate.parse("2023-01-13")] =
            wrkDay("07:30", "16:00", "PT8H", "PT30M")

        val result = WorkingTimeCalculator().distributeWorkingTime(input)
        result.forEach{ entry ->
            entry.value.last() shouldBe expectedOutput[entry.key]
        }
    }

    "distributeWorkingTime moves excess working time to two days" {
        val input = HashMap<LocalDate, List<WorkDaySummary>>()
        input[LocalDate.parse("2023-01-10")] = listOf(
            wrkDay("07:15", "18:00", "PT10H", "PT45M"),
            wrkDay("07:15", "18:00", "PT10H", "PT45M"))
        input[LocalDate.parse("2023-01-11")] = listOf(
            wrkDay("07:15", "19:00", "PT11H", "PT45M"),
            wrkDay("07:15", "18:00", "PT10H", "PT45M"))
        input[LocalDate.parse("2023-01-12")] = listOf(
            wrkDay("07:30", "17:45", "PT9H30M", "PT45M"),
            wrkDay("07:30", "17:45", "PT9H30M", "PT45M"))
        input[LocalDate.parse("2023-01-13")] = listOf(
            wrkDay("07:30", "16:00", "PT8H", "PT30M"),
            wrkDay("07:30", "16:00", "PT8H", "PT30M"))

        val expectedOutput = HashMap<LocalDate, WorkDaySummary>()
        expectedOutput[LocalDate.parse("2023-01-10")] =
            wrkDay("07:15", "18:00", "PT10H", "PT45M")
        expectedOutput[LocalDate.parse("2023-01-11")] =
            wrkDay("07:15", "18:00", "PT10H", "PT45M")
        expectedOutput[LocalDate.parse("2023-01-12")] =
            wrkDay("07:30", "18:15", "PT10H", "PT45M")
        expectedOutput[LocalDate.parse("2023-01-13")] =
            wrkDay("07:30", "16:30", "PT8H30M", "PT30M")

        val result = WorkingTimeCalculator().distributeWorkingTime(input)
        result.forEach{ entry ->
            entry.value.last() shouldBe expectedOutput[entry.key]
        }
    }

    "distributeWorkingTime moves excess working time to day before if following days to not allow it" {
        val input = HashMap<LocalDate, List<WorkDaySummary>>()
        input[LocalDate.parse("2023-01-10")] = listOf(
            wrkDay("07:00", "15:30", "PT8H", "PT30M"),
            wrkDay("07:00", "15:30", "PT8H", "PT30M"))
        input[LocalDate.parse("2023-01-11")] = listOf(
            wrkDay("07:00", "15:30", "PT8H", "PT30M"),
            wrkDay("07:00", "15:30", "PT8H", "PT30M"))
        input[LocalDate.parse("2023-01-12")] = listOf(
            wrkDay("07:00", "18:45", "PT11H", "PT45M"),
            wrkDay("07:00", "17:45", "PT10H", "PT45M"))
        input[LocalDate.parse("2023-01-14")] = listOf(
            wrkDay("07:00", "17:15", "PT9H30M", "PT45M"),
            wrkDay("07:00", "17:15", "PT9H30M", "PT45M"))

        val expectedOutput = HashMap<LocalDate, WorkDaySummary>()
        expectedOutput[LocalDate.parse("2023-01-10")] =
            wrkDay("07:00", "15:30", "PT8H", "PT30M")
        expectedOutput[LocalDate.parse("2023-01-11")] =
            wrkDay("07:00", "16:00", "PT8H30M", "PT30M")
        expectedOutput[LocalDate.parse("2023-01-12")] =
            wrkDay("07:00", "17:45", "PT10H", "PT45M")
        expectedOutput[LocalDate.parse("2023-01-14")] =
            wrkDay("07:00", "17:45", "PT10H", "PT45M")

        val result = WorkingTimeCalculator().distributeWorkingTime(input)
        result.forEach{ entry ->
            entry.value.last() shouldBe expectedOutput[entry.key]
        }
    }
})

fun wrkDay(cIn: String, cOut: String, wT: String, bT: String): WorkDaySummary {
    return WorkDaySummary(
        clockIn = LocalTime.parse(cIn),
        clockOut = LocalTime.parse(cOut),
        workingTime = Duration.parse(wT),
        breakTime = Duration.parse(bT),
    )
}

data class fun1Params(
    val clockIn: LocalTime,
    val clockOut: LocalTime,
    val additionalDuration: Duration,
)