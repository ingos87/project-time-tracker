package project.time.tracker.upload

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import project.time.tracker.*
import project.time.tracker.domain.CostAssessmentPosition
import java.time.Duration
import java.time.LocalDate
import kotlin.collections.HashMap

class CostAssessmentValidatorTests : FunSpec({

    beforeEach {
        ensureTestConfig("", "", "")
    }

    test("no moving of positions necessary") {
        val input = HashMap<LocalDate, List<CostAssessmentPosition>>()
        val list1 = listOf(
            CostAssessmentPosition(Duration.parse("PT1H"), "topic_1", "", ""),
            CostAssessmentPosition(Duration.parse("PT1H30M"), "topic_2", "", ""),
            CostAssessmentPosition(Duration.parse("PT6H"), "topic_3", "", ""),
        )
        val list2 = listOf(
            CostAssessmentPosition(Duration.parse("PT8H"), "topic_4", "", ""),
        )
        input[LocalDate.parse("2023-01-03")] = list1
        input[LocalDate.parse("2023-01-04")] = list2

        CostAssessmentValidator().moveProjectTimesToValidDays(input) shouldBe input
    }

    test("should move sunday hours to friday") {
        val input = HashMap<LocalDate, List<CostAssessmentPosition>>()
        val list0 = listOf(
            CostAssessmentPosition(Duration.parse("PT1H"), "topic_1", "", ""),
        )
        val list1 = listOf(
            CostAssessmentPosition(Duration.parse("PT1H"), "topic_1", "", ""),
            CostAssessmentPosition(Duration.parse("PT1H30M"), "topic_2", "", ""),
            CostAssessmentPosition(Duration.parse("PT6H"), "topic_3", "", ""),
        )
        val list2 = listOf(
            CostAssessmentPosition(Duration.parse("PT8H"), "topic_4", "", ""),
        )
        input[LocalDate.parse("2023-01-05")] = list0
        input[LocalDate.parse("2023-01-06")] = list1
        input[LocalDate.parse("2023-01-08")] = list2 // sunday

        val expectedOutput = mapOf<LocalDate, List<CostAssessmentPosition>>(
            LocalDate.parse("2023-01-05") to list0,
            LocalDate.parse("2023-01-06") to list1 + list2,
        )

        CostAssessmentValidator().moveProjectTimesToValidDays(input) shouldBe expectedOutput
    }

    test("should combine positions with same bookingKeys") {
        val input = HashMap<LocalDate, List<CostAssessmentPosition>>()
        val list1 = listOf(
            CostAssessmentPosition(Duration.parse("PT1H"), "projA", "code", "epp-1"),
            CostAssessmentPosition(Duration.parse("PT1H30M"), "projA", "code", "epp-123"),
        )
        val list2 = listOf(
            CostAssessmentPosition(Duration.parse("PT2H"), "projA", "code", "epp-1"),
        )
        input[LocalDate.parse("2023-05-01")] = list1 // public holiday
        input[LocalDate.parse("2023-05-02")] = list2

        val expectedOutput = mapOf<LocalDate, List<CostAssessmentPosition>>(
            LocalDate.parse("2023-05-02") to listOf(
                CostAssessmentPosition(Duration.parse("PT3H"), "projA", "code", "epp-1"),
                CostAssessmentPosition(Duration.parse("PT1H30M"), "projA", "code", "epp-123"),
            )
        )

        CostAssessmentValidator().moveProjectTimesToValidDays(input) shouldBe expectedOutput
    }
})

