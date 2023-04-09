package its.time.tracker.upload

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import its.time.tracker.*
import its.time.tracker.domain.ClockEvent
import its.time.tracker.domain.CostAssessmentPosition
import its.time.tracker.domain.EventType
import java.time.Duration
import java.time.LocalDateTime

class ProjectTimeCalculatorTests : FunSpec({

    beforeEach {
        ensureTestConfig("", "", "")
    }

    test("noop") {
        ProjectTimeCalculator().calculateProjectTime(emptyList(), false) shouldBe emptyList()
    }

    test("one element list") {
        val list = listOf(
            ClockEvent(LocalDateTime.parse("2023-01-03T12:00"), EventType.CLOCK_IN, "p1", "blubb"),
            ClockEvent(LocalDateTime.parse("2023-01-03T14:00"), EventType.CLOCK_OUT, "n/a", ""),
        )

        val expectedProjectTimes = listOf(
            CostAssessmentPosition("p1", Duration.parse("PT2H"), setOf("blubb")),
        )

        ProjectTimeCalculator().calculateProjectTime(list, false) shouldBe expectedProjectTimes
    }

    test("no clockout") {
        val list = listOf(
            ClockEvent(LocalDateTime.parse("2023-01-03T12:00"), EventType.CLOCK_IN, "p1", "blubb"),
        )

        val expectedProjectTimes = listOf(
            CostAssessmentPosition("p1", Duration.parse("PT9H"), setOf("blubb")),
        )

        ProjectTimeCalculator().calculateProjectTime(list, false) shouldBe expectedProjectTimes
    }

    test("no clockout with overtime") {
        val list = listOf(
            ClockEvent(LocalDateTime.parse("2023-01-03T07:00"), EventType.CLOCK_IN, "p1", "blubb"),
            ClockEvent(LocalDateTime.parse("2023-01-03T18:30"), EventType.CLOCK_IN, "p1", "blubb"),
        )

        val expectedProjectTimes = listOf(
            CostAssessmentPosition("p1", Duration.parse("PT12H"), setOf("blubb")),
        )

        ProjectTimeCalculator().calculateProjectTime(list, false) shouldBe expectedProjectTimes
    }

    test("no clockout, so clock-out now") {
        val list = listOf(
            ClockEvent(LocalDateTime.now().minusHours(1), EventType.CLOCK_IN, "p1", "blubb"),
        )

        val expectedProjectTimes = listOf(
            CostAssessmentPosition("p1", Duration.parse("PT1H"), setOf("blubb")),
        )

        ProjectTimeCalculator().calculateProjectTime(list, true) shouldBe expectedProjectTimes
    }

    test("multiple clock-outs") {
        val list = listOf(
            ClockEvent(LocalDateTime.parse("2023-01-03T12:00"), EventType.CLOCK_IN, "p1", "blubb"),
            ClockEvent(LocalDateTime.parse("2023-01-03T14:00"), EventType.CLOCK_OUT, "n/a", ""),
            ClockEvent(LocalDateTime.parse("2023-01-03T15:00"), EventType.CLOCK_IN, "p2", "blah"),
            ClockEvent(LocalDateTime.parse("2023-01-03T18:00"), EventType.CLOCK_OUT, "n/a", ""),
        )

        val expectedProjectTimes = listOf(
            CostAssessmentPosition("p1", Duration.parse("PT2H"), setOf("blubb")),
            CostAssessmentPosition("p2", Duration.parse("PT3H"), setOf("blah")),
        )

        ProjectTimeCalculator().calculateProjectTime(list, false) shouldBe expectedProjectTimes
    }

    test("unify works") {
        val list = listOf(
            ClockEvent(LocalDateTime.parse("2023-01-03T08:00"), EventType.CLOCK_IN, "p1", "blubb"),
            ClockEvent(LocalDateTime.parse("2023-01-03T09:00"), EventType.CLOCK_IN, "p2", "bums"),
            ClockEvent(LocalDateTime.parse("2023-01-03T10:00"), EventType.CLOCK_IN, "p3", "blubber"),
            ClockEvent(LocalDateTime.parse("2023-01-03T11:00"), EventType.CLOCK_IN, "p2", "blubb"),
            ClockEvent(LocalDateTime.parse("2023-01-03T12:00"), EventType.CLOCK_IN, "p1", "blah"),
            ClockEvent(LocalDateTime.parse("2023-01-03T13:00"), EventType.CLOCK_IN, "p1", "blaha"),
            ClockEvent(LocalDateTime.parse("2023-01-03T14:00"), EventType.CLOCK_IN, "p3", "blubber"),
            ClockEvent(LocalDateTime.parse("2023-01-03T15:00"), EventType.CLOCK_OUT, "n/a", ""),
        )

        val expectedProjectTimes = listOf(
            CostAssessmentPosition("p1", Duration.parse("PT3H"), setOf("blubb", "blah", "blaha")),
            CostAssessmentPosition("p2", Duration.parse("PT2H"), setOf("bums", "blubb")),
            CostAssessmentPosition("p3", Duration.parse("PT2H"), setOf("blubber")),
        )

        ProjectTimeCalculator().calculateProjectTime(list, false) shouldBe expectedProjectTimes
    }
})

