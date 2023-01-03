package its.time.tracker.service.util

import its.time.tracker.DATE_PATTERN
import its.time.tracker.MAX_WORK_HOURS_PER_DAY
import its.time.tracker.TIME_PATTERN
import its.time.tracker.service.util.DateTimeUtil.Companion.dateTimeToString
import its.time.tracker.service.util.DateTimeUtil.Companion.durationToString
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.Duration


class WorkTimeCalculator {

    fun calculateWorkTime(clockEvents: List<ClockEvent>): WorkTimeResult {
        if (clockEvents.isEmpty()) {
            return WorkTimeResult(
                firstClockIn = "00:00",
                lastClockOut = "00:00",
                totalWorkTime = "00:00",
                totalBreakTime = "00:00",)
        }

        var firstClockIn: LocalDateTime? = null
        var totalWorkDuration: Duration = Duration.ZERO
        var totalBreakDuration: Duration = Duration.ZERO

        var mostRecentClockIn: LocalDateTime? = null
        var mostRecentClockOut: LocalDateTime? = null

        var currentClockStatus = EventType.CLOCK_OUT

        clockEvents.forEach {
            if (it.eventType == EventType.CLOCK_IN) {
                if (firstClockIn == null) {
                    firstClockIn = it.dateTime
                    mostRecentClockIn = it.dateTime
                }
                else if (currentClockStatus == EventType.CLOCK_OUT) {
                    val breakDuration = Duration.between(mostRecentClockOut!!, it.dateTime)
                    totalBreakDuration = totalBreakDuration.plus(breakDuration)
                    mostRecentClockIn = it.dateTime
                }

                currentClockStatus = EventType.CLOCK_IN
            }
            else if (it.eventType == EventType.CLOCK_OUT) {
                if (currentClockStatus == EventType.CLOCK_IN) {
                    val workDuration = Duration.between(mostRecentClockIn!!, it.dateTime)
                    totalWorkDuration = totalWorkDuration.plus(workDuration)
                    mostRecentClockOut = it.dateTime
                }

                currentClockStatus = EventType.CLOCK_OUT
            }
        }

        if (currentClockStatus != EventType.CLOCK_OUT) {
            if (totalWorkDuration.toHours() >= MAX_WORK_HOURS_PER_DAY) {
                // although, this is beyond the max hours per day, any new tasks will take at least half an hour
                totalWorkDuration = totalWorkDuration.plus(30, ChronoUnit.MINUTES)
                mostRecentClockOut = mostRecentClockIn!!.plus(30, ChronoUnit.MINUTES)
            }
            else {
                val minutesTillMax = MAX_WORK_HOURS_PER_DAY * 60 - totalWorkDuration.toMinutes()

                totalWorkDuration = Duration.ofHours(MAX_WORK_HOURS_PER_DAY.toLong())
                mostRecentClockOut = mostRecentClockIn!!.plus(minutesTillMax, ChronoUnit.MINUTES)
            }
            println(dateTimeToString(clockEvents[0].dateTime, DATE_PATTERN) + ": No final clock-out found. Will insert one to fill up working time to ${durationToString(totalWorkDuration)} hours.")
        }

        return WorkTimeResult(
            firstClockIn = dateTimeToString(firstClockIn!!, TIME_PATTERN),
            lastClockOut = dateTimeToString(mostRecentClockOut!!, TIME_PATTERN),
            totalWorkTime = durationToString(totalWorkDuration),
            totalBreakTime = durationToString(totalBreakDuration))
    }
}

data class WorkTimeResult(
    val firstClockIn: String,
    val lastClockOut: String,
    val totalWorkTime: String,
    val totalBreakTime: String,
)