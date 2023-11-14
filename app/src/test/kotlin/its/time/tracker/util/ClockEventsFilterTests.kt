package its.time.tracker.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import its.time.tracker.domain.ClockEvent
import its.time.tracker.domain.EventType
import java.time.LocalDate
import java.time.LocalDateTime

class ClockEventsFilterTests : FunSpec({

    test("filter by day results in empty list if filter date is not among events") {
        val input = listOf(
            ClockEvent(LocalDateTime.parse("2023-01-03T12:00"), EventType.CLOCK_IN, "n/a", "blubb", "epp-123"),
            ClockEvent(LocalDateTime.parse("2023-01-03T14:00"), EventType.CLOCK_OUT, "n/a", "", "epp-123"),
            ClockEvent(LocalDateTime.parse("2023-01-03T23:00"), EventType.CLOCK_IN, "n/a", "blubb", "epp-123"),
            ClockEvent(LocalDateTime.parse("2023-01-04T00:30"), EventType.CLOCK_OUT, "n/a", "", "epp-123"),

            ClockEvent(LocalDateTime.parse("2023-01-05T11:00"), EventType.CLOCK_IN, "n/a", "blubb", "epp-123"),
        )

        val filterDate = LocalDate.parse("2023-01-06")
        ClockEventsFilter.getEventsBelongingToSameDay(input, filterDate) shouldBe emptyList()
    }

    test("filter by day results in empty list if no events are relevant") {
        val input = listOf(
            ClockEvent(LocalDateTime.parse("2023-01-03T12:00"), EventType.CLOCK_IN, "n/a", "blubb", "epp-123"),
            ClockEvent(LocalDateTime.parse("2023-01-03T14:00"), EventType.CLOCK_OUT, "n/a", "", "epp-123"),
            ClockEvent(LocalDateTime.parse("2023-01-03T23:00"), EventType.CLOCK_IN, "n/a", "blubb", "epp-123"),
            ClockEvent(LocalDateTime.parse("2023-01-04T00:30"), EventType.CLOCK_OUT, "n/a", "", "epp-123"),

            ClockEvent(LocalDateTime.parse("2023-01-05T11:00"), EventType.CLOCK_IN, "n/a", "blubb", "epp-123"),
        )

        val filterDate = LocalDate.parse("2023-01-04")
        ClockEventsFilter.getEventsBelongingToSameDay(input, filterDate) shouldBe emptyList()
    }

    test("filter by day works") {
        val input = listOf(
            ClockEvent(LocalDateTime.parse("2023-01-03T12:00"), EventType.CLOCK_IN, "n/a", "blubb", "epp-123"),
            ClockEvent(LocalDateTime.parse("2023-01-03T14:00"), EventType.CLOCK_OUT, "n/a", "", "epp-123"),
            ClockEvent(LocalDateTime.parse("2023-01-03T23:00"), EventType.CLOCK_IN, "n/a", "blubb", "epp-123"),
            ClockEvent(LocalDateTime.parse("2023-01-04T00:30"), EventType.CLOCK_OUT, "n/a", "", "epp-123"),

            ClockEvent(LocalDateTime.parse("2023-01-04T05:00"), EventType.CLOCK_IN, "n/a", "blubb", "epp-123"),
            ClockEvent(LocalDateTime.parse("2023-01-04T22:00"), EventType.CLOCK_IN, "n/a", "blubb", "epp-123"),
            ClockEvent(LocalDateTime.parse("2023-01-05T01:00"), EventType.CLOCK_OUT, "n/a", "blubb", "epp-123"),

            ClockEvent(LocalDateTime.parse("2023-01-05T11:00"), EventType.CLOCK_IN, "n/a", "blubb", "epp-123"),
        )

        val expectedResult = listOf(
            ClockEvent(LocalDateTime.parse("2023-01-04T05:00"), EventType.CLOCK_IN, "n/a", "blubb", "epp-123"),
            ClockEvent(LocalDateTime.parse("2023-01-04T22:00"), EventType.CLOCK_IN, "n/a", "blubb", "epp-123"),
            ClockEvent(LocalDateTime.parse("2023-01-05T01:00"), EventType.CLOCK_OUT, "n/a", "blubb", "epp-123"),
        )

        val filterDate = LocalDate.parse("2023-01-04")
        ClockEventsFilter.getEventsBelongingToSameDay(input, filterDate) shouldBe expectedResult
    }
})
