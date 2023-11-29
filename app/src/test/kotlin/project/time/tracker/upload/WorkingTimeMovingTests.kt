package project.time.tracker.upload

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import project.time.tracker.domain.WorkDaySummary
import project.time.tracker.domain.wrkDay
import project.time.tracker.ensureTestConfig
import java.time.LocalDate

class WorkingTimeMovingTests : StringSpec({

    beforeEach {
        ensureTestConfig("", "", "")
    }

    "ensureRestPeriodBetweenDays returns empty map" {
        val input = HashMap<LocalDate, List<WorkDaySummary>>().toSortedMap()
        WorkingTimeDistributer().ensureRestPeriodBetweenDays(input) shouldBe emptyMap()
    }

    "ensureRestPeriodBetweenDays does not move any data if all legal parameters are met" {
        val input = HashMap<LocalDate, List<WorkDaySummary>>().toSortedMap()
        input[LocalDate.parse("2023-01-10")] = listOf(
            wrkDay("2023-01-10", "10:30", "20:00", "PT9H", "PT30M"))
        input[LocalDate.parse("2023-01-11")] = listOf(
            wrkDay("2023-01-11", "07:00", "16:30", "PT9H", "PT30M"))
        input[LocalDate.parse("2023-01-12")] = listOf(
            wrkDay("2023-01-12", "07:30", "16:00", "PT8H", "PT30M"))

        val expectedOutput = HashMap<LocalDate, WorkDaySummary>()
        expectedOutput[LocalDate.parse("2023-01-10")] =
            wrkDay("2023-01-10", "10:30", "20:00", "PT9H", "PT30M")
        expectedOutput[LocalDate.parse("2023-01-11")] =
            wrkDay("2023-01-11", "07:00", "16:30", "PT9H", "PT30M")
        expectedOutput[LocalDate.parse("2023-01-12")] =
            wrkDay("2023-01-12", "07:30", "16:00", "PT8H", "PT30M")

        val result = WorkingTimeDistributer().ensureRestPeriodBetweenDays(input)
        result.forEach{ entry ->
            entry.value.last() shouldBe expectedOutput[entry.key]
        }
    }

    "ensureRestPeriodBetweenDays moves adjacent day's clockin and out" {
        val input = HashMap<LocalDate, List<WorkDaySummary>>().toSortedMap()
        input[LocalDate.parse("2023-01-10")] = listOf(
            wrkDay("2023-01-10", "10:30", "20:00", "PT9H", "PT30M"))
        input[LocalDate.parse("2023-01-11")] = listOf(
            wrkDay("2023-01-11", "06:00", "15:30", "PT9H", "PT30M"))
        input[LocalDate.parse("2023-01-12")] = listOf(
            wrkDay("2023-01-12", "07:30", "16:00", "PT8H", "PT30M"))

        val expectedOutput = HashMap<LocalDate, WorkDaySummary>()
        expectedOutput[LocalDate.parse("2023-01-10")] =
            wrkDay("2023-01-10", "09:30", "19:00", "PT9H", "PT30M")
        expectedOutput[LocalDate.parse("2023-01-11")] =
            wrkDay("2023-01-11", "06:00", "15:30", "PT9H", "PT30M")
        expectedOutput[LocalDate.parse("2023-01-12")] =
            wrkDay("2023-01-12", "07:30", "16:00", "PT8H", "PT30M")

        val result = WorkingTimeDistributer().ensureRestPeriodBetweenDays(input)
        result.forEach{ entry ->
            entry.value.last() shouldBe expectedOutput[entry.key]
        }
    }

    // this is only a theoretical case. in real life, all working times are already
    // compliant at this point of the process. Ergo, there can never be a work day with
    // more than 10h working time
    "ensureRestPeriodBetweenDays moves adjacent days' clockins and outs" {
        val input = HashMap<LocalDate, List<WorkDaySummary>>().toSortedMap()
        input[LocalDate.parse("2023-01-10")] = listOf(
            wrkDay("2023-01-10", "10:30", "20:00", "PT9H", "PT30M"))
        input[LocalDate.parse("2023-01-11")] = listOf(
            wrkDay("2023-01-11", "06:30", "20:30", "PT13H30M", "PT30M"))
        input[LocalDate.parse("2023-01-12")] = listOf(
            wrkDay("2023-01-12", "06:00", "16:00", "PT9H30M", "PT30M"))

        val expectedOutput = HashMap<LocalDate, WorkDaySummary>()
        expectedOutput[LocalDate.parse("2023-01-10")] =
            wrkDay("2023-01-10", "09:30", "19:00", "PT9H", "PT30M")
        expectedOutput[LocalDate.parse("2023-01-11")] =
            wrkDay("2023-01-11", "06:00", "20:00", "PT13H30M", "PT30M")
        expectedOutput[LocalDate.parse("2023-01-12")] =
            wrkDay("2023-01-12", "07:00", "17:00", "PT9H30M", "PT30M")

        val result = WorkingTimeDistributer().ensureRestPeriodBetweenDays(input)
        result.forEach{ entry ->
            entry.value.last() shouldBe expectedOutput[entry.key]
        }
    }
})