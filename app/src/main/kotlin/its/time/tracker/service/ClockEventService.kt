package its.time.tracker.service

import its.time.tracker.service.util.ClockEvent
import its.time.tracker.service.util.ClockOutType
import its.time.tracker.service.util.EventType
import java.time.LocalDateTime

class ClockEventService(
    private val verbose: Boolean,
    private val csvPath: String,
) {

    fun addClockOut(dateTime: LocalDateTime, clockOutType: ClockOutType = ClockOutType.MANUAL_CLOCK_OUT): Boolean {
        return addClockEvent(ClockEvent(dateTime, EventType.CLOCK_OUT, clockOutType.name))
    }

    fun addClockIn(topic: String, dateTime: LocalDateTime): Boolean {
        return addClockEvent(ClockEvent(dateTime, EventType.CLOCK_IN, topic))
    }

    private fun addClockEvent(clockEvent: ClockEvent): Boolean {
        val csvService = CsvService(verbose, csvPath)
        val clockEvents = csvService.loadClockEvents()

        val presentClockEvent = clockEvents.find { it.dateTime.isEqual(clockEvent.dateTime) }
        if (presentClockEvent != null) {
            if (presentClockEvent.eventType == clockEvent.eventType) {
                println("Will overwrite current event with identical time stamp: $presentClockEvent")
                clockEvents.remove(presentClockEvent)
            }
            else {
                println("Cannot overwrite event of different type. You must remove the present event before.")
                println("present: $presentClockEvent")
                println("new    : $clockEvent")
                return false
            }
        }

        clockEvents.add(clockEvent)

        csvService.saveClockEvents(clockEvents)

        return true
    }
}