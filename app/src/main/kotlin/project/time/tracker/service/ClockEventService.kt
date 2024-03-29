package project.time.tracker.service

import project.time.tracker.domain.ClockEvent
import project.time.tracker.domain.ClockOutType
import project.time.tracker.domain.EventType
import project.time.tracker.exception.AbortException
import java.time.LocalDateTime

class ClockEventService {

    fun addClockOut(dateTime: LocalDateTime, clockOutType: ClockOutType = ClockOutType.MANUAL_CLOCK_OUT) {
        return addClockEvent(ClockEvent(dateTime, EventType.CLOCK_OUT, "", clockOutType.name, ""))
    }

    fun addClockIn(project: String, topic: String?, story: String?, dateTime: LocalDateTime) {
        return addClockEvent(ClockEvent(dateTime, EventType.CLOCK_IN, project, topic?:"", story?:""))
    }

    private fun addClockEvent(clockEvent: ClockEvent) {
        val csvService = CsvService()
        val clockEvents = csvService.loadClockEvents()

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