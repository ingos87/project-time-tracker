package its.time.tracker.upload

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import its.time.tracker.domain.WorkDaySummary
import its.time.tracker.ensureTestConfig
import java.time.LocalDate

class WorkingTimeDistributionOverDaysTests : StringSpec({

    beforeEach {
        ensureTestConfig()
    }

    "ensureMaxWorkingTimePerDay returns empty map" {
        val input = HashMap<LocalDate, List<WorkDaySummary>>()
        WorkingTimeDistributer().ensureMaxWorkingTimePerDay(input) shouldBe emptyMap()
    }

    "ensureMaxWorkingTimePerDay does not alter data if all legal parameters are met" {
        val input = HashMap<LocalDate, List<WorkDaySummary>>()
        input[LocalDate.parse("2023-01-10")] = listOf(
            wrkDay("2023-01-10", "07:30", "16:00", "PT8H", "PT30M"))
        input[LocalDate.parse("2023-01-11")] = listOf(
            wrkDay("2023-01-11", "07:30", "16:00", "PT8H", "PT30M"))
        input[LocalDate.parse("2023-01-12")] = listOf(
            wrkDay("2023-01-12", "07:30", "16:00", "PT8H", "PT30M"))
        input[LocalDate.parse("2023-01-13")] = listOf(
            wrkDay("2023-01-13", "07:30", "16:00", "PT8H", "PT30M"))

        val expectedOutput = HashMap<LocalDate, WorkDaySummary>()
        expectedOutput[LocalDate.parse("2023-01-10")] =
            wrkDay("2023-01-10", "07:30", "16:00", "PT8H", "PT30M")
        expectedOutput[LocalDate.parse("2023-01-11")] =
            wrkDay("2023-01-11", "07:30", "16:00", "PT8H", "PT30M")
        expectedOutput[LocalDate.parse("2023-01-12")] =
            wrkDay("2023-01-12", "07:30", "16:00", "PT8H", "PT30M")
        expectedOutput[LocalDate.parse("2023-01-13")] =
            wrkDay("2023-01-13", "07:30", "16:00", "PT8H", "PT30M")

        val result = WorkingTimeDistributer().ensureMaxWorkingTimePerDay(input)
        result.forEach{ entry ->
            entry.value.last() shouldBe expectedOutput[entry.key]
        }
    }

    "ensureMaxWorkingTimePerDay moves excess working time to following day" {
        val input = HashMap<LocalDate, List<WorkDaySummary>>()
        input[LocalDate.parse("2023-01-10")] = listOf(
            wrkDay("2023-01-10", "07:15", "18:00", "PT10H", "PT45M"))
        input[LocalDate.parse("2023-01-11")] = listOf(
            wrkDay("2023-01-11", "07:15", "19:00", "PT11H", "PT45M"))
        input[LocalDate.parse("2023-01-12")] = listOf(
            wrkDay("2023-01-12", "07:30", "15:00", "PT7H", "PT30M"))
        input[LocalDate.parse("2023-01-13")] = listOf(
            wrkDay("2023-01-13", "07:30", "16:00", "PT8H", "PT30M"))
        
        val expectedOutput = HashMap<LocalDate, WorkDaySummary>()
        expectedOutput[LocalDate.parse("2023-01-10")] =
            wrkDay("2023-01-10", "07:15", "18:00", "PT10H", "PT45M")
        expectedOutput[LocalDate.parse("2023-01-11")] =
            wrkDay("2023-01-11", "07:15", "18:00", "PT10H", "PT45M")
        expectedOutput[LocalDate.parse("2023-01-12")] =
            wrkDay("2023-01-12", "07:30", "16:00", "PT8H", "PT30M")
        expectedOutput[LocalDate.parse("2023-01-13")] =
            wrkDay("2023-01-13", "07:30", "16:00", "PT8H", "PT30M")

        val result = WorkingTimeDistributer().ensureMaxWorkingTimePerDay(input)
        result.forEach{ entry ->
            entry.value.last() shouldBe expectedOutput[entry.key]
        }
    }

    "ensureMaxWorkingTimePerDay moves excess working time to two days" {
        val input = HashMap<LocalDate, List<WorkDaySummary>>()
        input[LocalDate.parse("2023-01-10")] = listOf(
            wrkDay("2023-01-10", "07:15", "18:00", "PT10H", "PT45M"))
        input[LocalDate.parse("2023-01-11")] = listOf(
            wrkDay("2023-01-11", "07:15", "19:00", "PT11H", "PT45M"))
        input[LocalDate.parse("2023-01-12")] = listOf(
            wrkDay("2023-01-12", "07:30", "17:45", "PT9H30M", "PT45M"))
        input[LocalDate.parse("2023-01-13")] = listOf(
            wrkDay("2023-01-13", "07:30", "16:00", "PT8H", "PT30M"))

        val expectedOutput = HashMap<LocalDate, WorkDaySummary>()
        expectedOutput[LocalDate.parse("2023-01-10")] =
            wrkDay("2023-01-10", "07:15", "18:00", "PT10H", "PT45M")
        expectedOutput[LocalDate.parse("2023-01-11")] =
            wrkDay("2023-01-11", "07:15", "18:00", "PT10H", "PT45M")
        expectedOutput[LocalDate.parse("2023-01-12")] =
            wrkDay("2023-01-12", "07:30", "18:15", "PT10H", "PT45M")
        expectedOutput[LocalDate.parse("2023-01-13")] =
            wrkDay("2023-01-13", "07:30", "16:30", "PT8H30M", "PT30M")

        val result = WorkingTimeDistributer().ensureMaxWorkingTimePerDay(input)
        result.forEach{ entry ->
            entry.value.last() shouldBe expectedOutput[entry.key]
        }
    }

    "ensureMaxWorkingTimePerDay moves excess working time to day before if following days to not allow it" {
        val input = HashMap<LocalDate, List<WorkDaySummary>>()
        input[LocalDate.parse("2023-01-10")] = listOf(
            wrkDay("2023-01-10", "07:00", "15:30", "PT8H", "PT30M"))
        input[LocalDate.parse("2023-01-11")] = listOf(
            wrkDay("2023-01-11", "07:00", "15:30", "PT8H", "PT30M"))
        input[LocalDate.parse("2023-01-12")] = listOf(
            wrkDay("2023-01-12", "07:00", "18:45", "PT11H", "PT45M"))
        input[LocalDate.parse("2023-01-13")] = listOf(
            wrkDay("2023-01-13", "07:00", "17:15", "PT9H30M", "PT45M"))

        val expectedOutput = HashMap<LocalDate, WorkDaySummary>()
        expectedOutput[LocalDate.parse("2023-01-10")] =
            wrkDay("2023-01-10", "07:00", "15:30", "PT8H", "PT30M")
        expectedOutput[LocalDate.parse("2023-01-11")] =
            wrkDay("2023-01-11", "07:00", "16:00", "PT8H30M", "PT30M")
        expectedOutput[LocalDate.parse("2023-01-12")] =
            wrkDay("2023-01-12", "07:00", "17:45", "PT10H", "PT45M")
        expectedOutput[LocalDate.parse("2023-01-13")] =
            wrkDay("2023-01-13", "07:00", "17:45", "PT10H", "PT45M")

        val result = WorkingTimeDistributer().ensureMaxWorkingTimePerDay(input)
        result.forEach{ entry ->
            entry.value.last() shouldBe expectedOutput[entry.key]
        }
    }

    "move working time from non-working days to working days" {
        ensureTestConfig("2023-05-03")

        val input = HashMap<LocalDate, List<WorkDaySummary>>()
        input[LocalDate.parse("2023-05-01")] = listOf(
            wrkDay("2023-05-01", "07:00", "09:00", "PT2H", "PT0M"))
        input[LocalDate.parse("2023-05-02")] = listOf(
            wrkDay("2023-05-02", "10:00", "12:00", "PT2H", "PT0M"))
        input[LocalDate.parse("2023-05-03")] = listOf(
            wrkDay("2023-05-03", "08:00", "10:00", "PT2H", "PT0M"))
        input[LocalDate.parse("2023-05-04")] = listOf(
            wrkDay("2023-05-04", "10:00", "12:00", "PT2H", "PT0M"))
        input[LocalDate.parse("2023-05-05")] = listOf(
            wrkDay("2023-05-05", "10:00", "12:00", "PT2H", "PT0M"))
        input[LocalDate.parse("2023-05-06")] = listOf(
            wrkDay("2023-05-06", "09:00", "11:00", "PT2H", "PT0M"))

        val expectedOutput = HashMap<LocalDate, WorkDaySummary>()
        expectedOutput[LocalDate.parse("2023-05-01")] =
            wrkDay("2023-05-01", "12:00", "12:00", "PT0H", "PT0M") // holiday
        expectedOutput[LocalDate.parse("2023-05-02")] =
            wrkDay("2023-05-02", "10:00", "14:00", "PT4H", "PT0M")
        expectedOutput[LocalDate.parse("2023-05-03")] =
            wrkDay("2023-05-03", "12:00", "12:00", "PT0H", "PT0M") // day off
        expectedOutput[LocalDate.parse("2023-05-04")] =
            wrkDay("2023-05-04", "10:00", "14:00", "PT4H", "PT0M")
        expectedOutput[LocalDate.parse("2023-05-05")] =
            wrkDay("2023-05-05", "10:00", "14:00", "PT4H", "PT0M")
        expectedOutput[LocalDate.parse("2023-05-06")] =
            wrkDay("2023-05-06", "12:00", "12:00", "PT0H", "PT0M") // saturday

        val result = WorkingTimeDistributer().ensureMaxWorkingTimePerDay(input)
        result.forEach{ entry ->
            entry.value.last() shouldBe expectedOutput[entry.key]
        }
    }
})