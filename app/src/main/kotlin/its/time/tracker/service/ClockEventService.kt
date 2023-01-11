package its.time.tracker.service

import its.time.tracker.service.util.*
import java.time.LocalDate
import java.time.LocalDateTime

class ClockEventService(
    private val verbose: Boolean,
    private val csvPath: String,
) {

    fun addClockOut(dateTime: LocalDateTime, clockOutType: ClockOutType = ClockOutType.MANUAL_CLOCK_OUT) {
        return addClockEvent(ClockEvent(dateTime, EventType.CLOCK_OUT, clockOutType.name))
    }

    fun addClockIn(topic: String, dateTime: LocalDateTime) {
        return addClockEvent(ClockEvent(dateTime, EventType.CLOCK_IN, topic))
    }

    fun addFlexDay(date: LocalDate) {
        val csvService = CsvService(verbose, csvPath)
        val clockEvents = csvService.loadClockEvents()

        val daysClockIns = clockEvents.filter { DateTimeUtil.isSameDay(it.dateTime, date) && it.eventType == EventType.CLOCK_IN}
        if (daysClockIns.isNotEmpty()) {
            throw AbortException("Flex time is not possible for ${DateTimeUtil.temporalToString(date, DATE_PATTERN)} due to existing clock-in(s)")
        }

        clockEvents.add(
            ClockEvent(
                dateTime = date.atStartOfDay(),
                eventType = EventType.FLEX_TIME,
                ""
            )
        )

        csvService.saveClockEvents(clockEvents)
    }

    private fun addClockEvent(clockEvent: ClockEvent) {
        val csvService = CsvService(verbose, csvPath)
        val clockEvents = csvService.loadClockEvents()

        val existingFlexTime = clockEvents.find { it.eventType == EventType.FLEX_TIME && DateTimeUtil.isSameDay(clockEvent.dateTime, it.dateTime.toLocalDate()) }
        if (existingFlexTime != null && clockEvent.eventType == EventType.CLOCK_IN) {
            throw AbortException("Clock-in is not possible on ${DateTimeUtil.temporalToString(clockEvent.dateTime, DATE_PATTERN)} due to present flex time.")
        }

        val existingClockEvent = clockEvents.find { it.dateTime.isEqual(clockEvent.dateTime) }
        if (existingClockEvent != null) {
            if (existingClockEvent.eventType == clockEvent.eventType) {
                println("Will overwrite current event with identical time stamp: $existingClockEvent")
                clockEvents.remove(existingClockEvent)
            }
            else {
                throw AbortException(
                    "Cannot overwrite event of different type. You must remove the present event before.",
                    listOf("present: $existingClockEvent",
                           "new    : $clockEvent"))
            }
        }

        clockEvents.add(clockEvent)

        csvService.saveClockEvents(clockEvents)
    }
}