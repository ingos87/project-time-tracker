package its.time.tracker

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import its.time.tracker.service.util.WorkingTimeDistributionService
import its.time.tracker.service.util.WorkDaySummary
import java.time.LocalDate

class WorkingTimeDistributionOverDaysTests : StringSpec({

    beforeEach {
        ensureTestConfig("MON,SAT,SUN")
    }

    "ensureMaxWorkingTimePerDay returns empty map" {
        val input = HashMap<LocalDate, List<WorkDaySummary>>()
        WorkingTimeDistributionService().ensureMaxWorkingTimePerDay(input) shouldBe emptyMap()
    }

    "ensureMaxWorkingTimePerDay does not alter data if all legal parameters are met" {
        val input = HashMap<LocalDate, List<WorkDaySummary>>()
        input[LocalDate.parse("2023-01-10")] = listOf(
            wrkDay("07:30", "16:00", "PT8H", "PT30M"))
        input[LocalDate.parse("2023-01-11")] = listOf(
            wrkDay("07:30", "16:00", "PT8H", "PT30M"))
        input[LocalDate.parse("2023-01-12")] = listOf(
            wrkDay("07:30", "16:00", "PT8H", "PT30M"))
        input[LocalDate.parse("2023-01-13")] = listOf(
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

        val result = WorkingTimeDistributionService().ensureMaxWorkingTimePerDay(input)
        result.forEach{ entry ->
            entry.value.last() shouldBe expectedOutput[entry.key]
        }
    }

    "ensureMaxWorkingTimePerDay moves excess working time to following day" {
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

        val result = WorkingTimeDistributionService().ensureMaxWorkingTimePerDay(input)
        result.forEach{ entry ->
            entry.value.last() shouldBe expectedOutput[entry.key]
        }
    }

    "ensureMaxWorkingTimePerDay moves excess working time to two days" {
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

        val result = WorkingTimeDistributionService().ensureMaxWorkingTimePerDay(input)
        result.forEach{ entry ->
            entry.value.last() shouldBe expectedOutput[entry.key]
        }
    }

    "ensureMaxWorkingTimePerDay moves excess working time to day before if following days to not allow it" {
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

        val result = WorkingTimeDistributionService().ensureMaxWorkingTimePerDay(input)
        result.forEach{ entry ->
            entry.value.last() shouldBe expectedOutput[entry.key]
        }
    }
})