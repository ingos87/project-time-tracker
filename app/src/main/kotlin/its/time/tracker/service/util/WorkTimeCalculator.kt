package its.time.tracker.service.util

import its.time.tracker.MAX_WORK_HOURS_PER_DAY
import its.time.tracker.service.util.DateTimeUtil.Companion.temporalToString
import its.time.tracker.service.util.DateTimeUtil.Companion.durationToString
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.MINUTES
import java.time.Duration


class WorkTimeCalculator {

    fun calculateWorkTime(clockEvents: List<ClockEvent>, useNowAsCLockOut: Boolean = false): WorkTimeResult {
        if (clockEvents.isEmpty()) {
            return WorkTimeResult(
                firstClockIn = "00:00",
                lastClockOut = "00:00",
                totalWorkTime = "00:00",
                totalBreakTime = "00:00",)
        }

        val flexTimeEvent = clockEvents.find { it.eventType == EventType.FLEX_TIME }
        if (flexTimeEvent != null) {
            return WorkTimeResult(
                firstClockIn = "flex",
                lastClockOut = "flex",
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
            if (useNowAsCLockOut) {
                val now = LocalDateTime.now()
                totalWorkDuration = totalWorkDuration.plusMinutes(MINUTES.between(mostRecentClockIn, now))
                mostRecentClockOut = now
            }
            else if (totalWorkDuration.toHours() >= MAX_WORK_HOURS_PER_DAY) {
                // although, this is beyond the max hours per day, any new tasks will take at least half an hour
                totalWorkDuration = totalWorkDuration.plusMinutes(30)
                mostRecentClockOut = mostRecentClockIn!!.plusMinutes(30)

                println(temporalToString(clockEvents[0].dateTime, DATE_PATTERN) + ": No final clock-out found. Will insert one. Work time will be ${durationToString(totalWorkDuration)} hours.")
            }
            else {
                val minutesTillMax = MAX_WORK_HOURS_PER_DAY * 60 - totalWorkDuration.toMinutes()

                totalWorkDuration = Duration.ofHours(MAX_WORK_HOURS_PER_DAY.toLong())
                mostRecentClockOut = mostRecentClockIn!!.plusMinutes(minutesTillMax)
                println(temporalToString(clockEvents[0].dateTime, DATE_PATTERN) + ": No final clock-out found. Will insert one to fill up working time to maximum (${durationToString(totalWorkDuration)} hours).")
            }
        }

        return WorkTimeResult(
            firstClockIn = temporalToString(firstClockIn!!, TIME_PATTERN),
            lastClockOut = temporalToString(mostRecentClockOut!!, TIME_PATTERN),
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