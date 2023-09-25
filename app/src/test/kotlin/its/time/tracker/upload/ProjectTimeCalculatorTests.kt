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
            ClockEvent(LocalDateTime.parse("2023-01-03T12:00"), EventType.CLOCK_IN, "p1", "blubb", "epp-123"),
            ClockEvent(LocalDateTime.parse("2023-01-03T14:00"), EventType.CLOCK_OUT, "n/a", "", ""),
        )

        val expectedProjectTimes = listOf(
            CostAssessmentPosition(Duration.parse("PT2H"), "p1", "blubb", "epp-123"),
        )

        ProjectTimeCalculator().calculateProjectTime(list, false) shouldBe expectedProjectTimes
    }

    test("no clockout") {
        val list = listOf(
            ClockEvent(LocalDateTime.parse("2023-01-03T12:00"), EventType.CLOCK_IN, "p1", "blubb", "epp-123"),
        )

        val expectedProjectTimes = listOf(
            CostAssessmentPosition(Duration.parse("PT9H"), "p1", "blubb", "epp-123"),
        )

        ProjectTimeCalculator().calculateProjectTime(list, false) shouldBe expectedProjectTimes
    }

    test("no clockout with overtime") {
        val list = listOf(
            ClockEvent(LocalDateTime.parse("2023-01-03T07:00"), EventType.CLOCK_IN, "p1", "blubb", "epp-123"),
            ClockEvent(LocalDateTime.parse("2023-01-03T18:30"), EventType.CLOCK_IN, "p1", "blubb", "epp-123"),
        )

        val expectedProjectTimes = listOf(
            CostAssessmentPosition(Duration.parse("PT12H"), "p1", "blubb", "epp-123"),
        )

        ProjectTimeCalculator().calculateProjectTime(list, false) shouldBe expectedProjectTimes
    }

    test("no clockout, so clock-out now") {
        val list = listOf(
            ClockEvent(LocalDateTime.now().minusHours(1), EventType.CLOCK_IN, "p1", "blubb", "epp-123"),
        )

        val expectedProjectTimes = listOf(
            CostAssessmentPosition(Duration.parse("PT1H"), "p1", "blubb", "epp-123"),
        )

        ProjectTimeCalculator().calculateProjectTime(list, true) shouldBe expectedProjectTimes
    }

    test("multiple clock-outs") {
        val list = listOf(
            ClockEvent(LocalDateTime.parse("2023-01-03T12:00"), EventType.CLOCK_IN, "p1", "blubb", "epp-123"),
            ClockEvent(LocalDateTime.parse("2023-01-03T14:00"), EventType.CLOCK_OUT, "n/a", "", "epp-123"),
            ClockEvent(LocalDateTime.parse("2023-01-03T15:00"), EventType.CLOCK_IN, "p2", "blah", "epp-123"),
            ClockEvent(LocalDateTime.parse("2023-01-03T18:00"), EventType.CLOCK_OUT, "n/a", "", "epp-123"),
        )

        val expectedProjectTimes = listOf(
            CostAssessmentPosition(Duration.parse("PT2H"), "p1", "blubb", "epp-123"),
            CostAssessmentPosition(Duration.parse("PT3H"), "p2", "blah", "epp-123"),
        )

        ProjectTimeCalculator().calculateProjectTime(list, false) shouldBe expectedProjectTimes
    }

    test("unify works") {
        val list = listOf(
            ClockEvent(LocalDateTime.parse("2023-01-03T08:00"), EventType.CLOCK_IN, "p1", "meeting", ""),
            ClockEvent(LocalDateTime.parse("2023-01-03T09:00"), EventType.CLOCK_IN, "p2", "meeting", ""),
            ClockEvent(LocalDateTime.parse("2023-01-03T10:00"), EventType.CLOCK_IN, "p3", "code", "epp-123"),
            ClockEvent(LocalDateTime.parse("2023-01-03T11:00"), EventType.CLOCK_IN, "p2", "code", "epp-456"),
            ClockEvent(LocalDateTime.parse("2023-01-03T12:00"), EventType.CLOCK_IN, "p1", "meeting", "epp-7"),
            ClockEvent(LocalDateTime.parse("2023-01-03T13:00"), EventType.CLOCK_IN, "p1", "code", "epp-7"),
            ClockEvent(LocalDateTime.parse("2023-01-03T14:00"), EventType.CLOCK_IN, "p3", "code", "epp-123"),
            ClockEvent(LocalDateTime.parse("2023-01-03T15:00"), EventType.CLOCK_IN, "p1", "meeting", "epp-7"),
            ClockEvent(LocalDateTime.parse("2023-01-03T16:00"), EventType.CLOCK_OUT, "n/a", "", ""),
        )

        val expectedProjectTimes = listOf(
            CostAssessmentPosition(Duration.parse("PT1H"), "p1", "meeting", ""),
            CostAssessmentPosition(Duration.parse("PT1H"), "p2", "meeting", ""),
            CostAssessmentPosition(Duration.parse("PT2H"), "p3", "code", "epp-123"),
            CostAssessmentPosition(Duration.parse("PT1H"), "p2", "code", "epp-456"),
            CostAssessmentPosition(Duration.parse("PT2H"), "p1", "meeting", "epp-7"),
            CostAssessmentPosition(Duration.parse("PT1H"), "p1", "code", "epp-7"),
        )

        ProjectTimeCalculator().calculateProjectTime(list, false) shouldBe expectedProjectTimes
    }
})

