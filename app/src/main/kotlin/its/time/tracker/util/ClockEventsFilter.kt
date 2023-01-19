package its.time.tracker.util

import its.time.tracker.domain.ClockEvent
import its.time.tracker.domain.EventType
import java.time.LocalDate

class ClockEventsFilter {
    companion object {
        fun getEventsBelongingToSameDay(input: List<ClockEvent>, date: LocalDate): List<ClockEvent> {
            val relevantIndices = input
                .mapIndexed { index, clockEvent -> index to clockEvent }.toMap()
                .filter { entry -> entry.value.dateTime.toLocalDate().isEqual(date) }
                .map { entry -> entry.key }.toMutableList()

            return filterInternal(input, relevantIndices)
        }

        fun getEventsBelongingToMonth(input: List<ClockEvent>, date: LocalDate): List<ClockEvent> {
            val relevantIndices = input
                .mapIndexed { index, clockEvent -> index to clockEvent }.toMap()
                .filter { entry -> entry.value.dateTime.year == date.year
                        && entry.value.dateTime.month == date.month }
                .map { entry -> entry.key }.toMutableList()

            return filterInternal(input, relevantIndices)
        }

        private fun filterInternal(input: List<ClockEvent>, relevantIndices: MutableList<Int>): List<ClockEvent> {
            if (relevantIndices.isEmpty()) {
                return emptyList()
            }

            val nextIndex = relevantIndices.last() + 1
            if (input.size > nextIndex
                && input[nextIndex].eventType == EventType.CLOCK_OUT) {
                relevantIndices.add(nextIndex)
            }

            if (relevantIndices.isNotEmpty()
                && input[relevantIndices[0]].eventType == EventType.CLOCK_OUT) {
                relevantIndices.removeFirst()
            }

            return input.filterIndexed{index, _ -> relevantIndices.contains(index)}
        }
    }
}