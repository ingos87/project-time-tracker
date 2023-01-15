package its.time.tracker

import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import its.time.tracker.service.util.WorkingTimeDistributionCalculator
import its.time.tracker.service.util.WorkDaySummary
import its.time.tracker.service.util.WorkingTimeCalculator
import java.time.Duration
import java.time.LocalDate

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
            WorkingTimeCalculator.getTotalBreakDuration(workingTime) shouldBe totalBreakTime
        }
    }

    "distributeWorkingTime returns empty map" {
        val input = HashMap<LocalDate, List<WorkDaySummary>>()
        WorkingTimeDistributionCalculator().distributeWorkingTime(input) shouldBe emptyMap()
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

        val result = WorkingTimeDistributionCalculator().distributeWorkingTime(input)
        result.forEach{ entry ->
            entry.value.last() shouldBe expectedOutput[entry.key]
        }
    }

    "distributeWorkingTime moves excess working time to following day" {
        val input = HashMap<LocalDate, List<WorkDaySummary>>()
        input[LocalDate.parse("2023-01-10")] = listOf(
            wrkDay("07:15", "18:00", "PT10H", "PT45M"))
        input[LocalDate.parse("2023-01-11")] = listOf(
            wrkDay("07:15", "19:00", "PT11H", "PT45M"))
        input[LocalDate.parse("2023-01-12")] = listOf(
            wrkDay("07:30", "15:00", "PT7H", "PT30M"))
        input[LocalDate.parse("2023-01-13")] = listOf(
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

        val result = WorkingTimeDistributionCalculator().distributeWorkingTime(input)
        result.forEach{ entry ->
            entry.value.last() shouldBe expectedOutput[entry.key]
        }
    }

    "distributeWorkingTime moves excess working time to two days" {
        val input = HashMap<LocalDate, List<WorkDaySummary>>()
        input[LocalDate.parse("2023-01-10")] = listOf(
            wrkDay("07:15", "18:00", "PT10H", "PT45M"))
        input[LocalDate.parse("2023-01-11")] = listOf(
            wrkDay("07:15", "19:00", "PT11H", "PT45M"))
        input[LocalDate.parse("2023-01-12")] = listOf(
            wrkDay("07:30", "17:45", "PT9H30M", "PT45M"))
        input[LocalDate.parse("2023-01-13")] = listOf(
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

        val result = WorkingTimeDistributionCalculator().distributeWorkingTime(input)
        result.forEach{ entry ->
            entry.value.last() shouldBe expectedOutput[entry.key]
        }
    }

    "distributeWorkingTime moves excess working time to day before if following days to not allow it" {
        val input = HashMap<LocalDate, List<WorkDaySummary>>()
        input[LocalDate.parse("2023-01-10")] = listOf(
            wrkDay("07:00", "15:30", "PT8H", "PT30M"))
        input[LocalDate.parse("2023-01-11")] = listOf(
            wrkDay("07:00", "15:30", "PT8H", "PT30M"))
        input[LocalDate.parse("2023-01-12")] = listOf(
            wrkDay("07:00", "18:45", "PT11H", "PT45M"))
        input[LocalDate.parse("2023-01-14")] = listOf(
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

        val result = WorkingTimeDistributionCalculator().distributeWorkingTime(input)
        result.forEach{ entry ->
            entry.value.last() shouldBe expectedOutput[entry.key]
        }
    }
})