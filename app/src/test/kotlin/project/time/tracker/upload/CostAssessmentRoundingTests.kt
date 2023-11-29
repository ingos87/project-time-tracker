package project.time.tracker.upload

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import project.time.tracker.*
import project.time.tracker.domain.CostAssessmentPosition
import java.time.Duration
import java.time.LocalDate
import kotlin.collections.HashMap

class CostAssessmentRoundingTests : FunSpec({

    beforeEach {
        ensureTestConfig("", "", "")
    }

    test("no rounding necessary") {
        val input = HashMap<LocalDate, List<CostAssessmentPosition>>()
        val list1 = listOf(
            CostAssessmentPosition(Duration.parse("PT1H"), "topic_1", "", ""),
            CostAssessmentPosition(Duration.parse("PT1H30M"), "topic_2", "", ""),
            CostAssessmentPosition(Duration.parse("PT6H"), "topic_3", "", ""),
        )
        input[LocalDate.parse("2023-01-03")] = list1

        CostAssessmentRoundingService().roundProjectTimes(input.toSortedMap()) shouldBe input
    }

    test("rounding works within one day") {
        val input = HashMap<LocalDate, List<CostAssessmentPosition>>()
        val list1 = listOf(
            CostAssessmentPosition(Duration.parse("PT1H"), "topic_1", "", ""),
            CostAssessmentPosition(Duration.parse("PT1H33M"), "topic_2", "", ""),
            CostAssessmentPosition(Duration.parse("PT6H14M"), "topic_3", "", ""),
        )
        input[LocalDate.parse("2023-01-03")] = list1


        val expectedOutput = HashMap<LocalDate, List<CostAssessmentPosition>>()
        val listOut1 = listOf(
            CostAssessmentPosition(Duration.parse("PT1H"), "topic_1", "", ""),
            CostAssessmentPosition(Duration.parse("PT1H30M"), "topic_2", "", ""),
            CostAssessmentPosition(Duration.parse("PT6H"), "topic_3", "", ""),
        )
        expectedOutput[LocalDate.parse("2023-01-03")] = listOut1

        val output = CostAssessmentRoundingService().roundProjectTimes(input.toSortedMap())
        output.size shouldBe expectedOutput.size
        expectedOutput.forEach { (k, v) ->
            output.containsKey(k) shouldBe true
            output[k] shouldBe expectedOutput[k]
        }
    }
})

