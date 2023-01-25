package its.time.tracker.upload

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import its.time.tracker.*
import its.time.tracker.domain.CostAssessmentPosition
import java.time.Duration
import java.time.LocalDate
import kotlin.collections.HashMap

class CostAssessmentRoundingTests : FunSpec({

    beforeEach {
        ensureTestConfig()
    }

    test("no rounding necessary") {
        val input = HashMap<LocalDate, List<CostAssessmentPosition>>()
        val list1 = listOf(
            CostAssessmentPosition("topic_1", Duration.parse("PT1H"), emptySet()),
            CostAssessmentPosition("topic_2", Duration.parse("PT1H30M"), emptySet()),
            CostAssessmentPosition("topic_3", Duration.parse("PT6H"), emptySet()),
        )
        input[LocalDate.parse("2023-01-03")] = list1

        CostAssessmentRoundingService().roundProjectTimes(input.toSortedMap()) shouldBe input
    }

    test("rounding works within one day") {
        val input = HashMap<LocalDate, List<CostAssessmentPosition>>()
        val list1 = listOf(
            CostAssessmentPosition("topic_1", Duration.parse("PT1H"), emptySet()),
            CostAssessmentPosition("topic_2", Duration.parse("PT1H33M"), emptySet()),
            CostAssessmentPosition("topic_3", Duration.parse("PT6H14M"), emptySet()),
        )
        input[LocalDate.parse("2023-01-03")] = list1


        val expectedOutput = HashMap<LocalDate, List<CostAssessmentPosition>>()
        val listOut1 = listOf(
            CostAssessmentPosition("topic_1", Duration.parse("PT1H"), emptySet()),
            CostAssessmentPosition("topic_2", Duration.parse("PT1H30M"), emptySet()),
            CostAssessmentPosition("topic_3", Duration.parse("PT6H"), emptySet()),
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

