package project.time.tracker.util

import project.time.tracker.domain.ClockEvent
import project.time.tracker.domain.EventType
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
        fun getEventsBelongingToDateRange(input: List<ClockEvent>,
                                          firstDate: LocalDate,
                                          lastDate: LocalDate): List<ClockEvent> {
            val relevantIndices = input
                .mapIndexed { index, clockEvent -> index to clockEvent }.toMap()
                .filter { entry ->
                    entry.value.dateTime.toLocalDate().isEqual(firstDate) ||
                    entry.value.dateTime.toLocalDate().isEqual(lastDate) || (
                    entry.value.dateTime.toLocalDate().isAfter(firstDate) &&
                    entry.value.dateTime.toLocalDate().isBefore(lastDate))
                  }
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