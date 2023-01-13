package its.time.tracker.service.util

import its.time.tracker.MAX_WORK_HOURS_PER_DAY
import its.time.tracker.service.util.DateTimeUtil.Companion.temporalToString
import its.time.tracker.service.util.DateTimeUtil.Companion.durationToString
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.MINUTES
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.HashMap

/**
 * MySelfHr/ArbSG rules
 * no work before 06:00
 * no work after 21:00
 * break between days: 11 hours
 * minimum break time after 6h work time: 30min
 * minimum break time after another 3h work time: 15min (45min in total)
 */
val EARLIEST_START_OF_DAY = LocalTime.parse("06:00")
val LATEST_END_OF_DAY = LocalTime.parse("06:00")
val MIN_BREAK_BTW_DAYS = Duration.ofHours(11)
val MAX_WORK_BEFORE_BREAK1 = Duration.ofHours(6)
val MAX_WORK_BEFORE_BREAK2 = Duration.ofHours(9)

class WorkingTimeCalculator {

    fun calculateWorkingTime(clockEvents: List<ClockEvent>, useNowAsCLockOut: Boolean = false): WorkingTimeResult {
        if (clockEvents.isEmpty()) {
            return WorkingTimeResult(
                firstClockIn = LocalTime.parse("00:00"),
                lastClockOut = LocalTime.parse("00:00"),
                totalWorkingTime = Duration.ZERO,
                totalBreakTime = Duration.ZERO,
            )
        }

        val flexTimeEvent = clockEvents.find { it.eventType == EventType.FLEX_TIME }
        if (flexTimeEvent != null) {
            return WorkingTimeResult(
                firstClockIn = null,
                lastClockOut = null,
                totalWorkingTime = Duration.ZERO,
                totalBreakTime = Duration.ZERO,
            )
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

        return WorkingTimeResult(
            firstClockIn = firstClockIn?.toLocalTime(),
            lastClockOut = mostRecentClockOut?.toLocalTime(),
            totalWorkingTime = totalWorkDuration,
            totalBreakTime = totalBreakDuration
        )
    }

    fun normalizeWeekWorkingTime(workingTimeResults: HashMap<LocalDate, WorkingTimeResult>): HashMap<LocalDate, CompliantWorkingTime> {

        TODO("Not yet implemented")
    }


    fun toCompliantWorkingTime(workingTimeResult: WorkingTimeResult): CompliantWorkingTime {

        var compliantClockOut = workingTimeResult.lastClockOut
        if (compliantClockOut != null && workingTimeResult.totalWorkingTime > MAX_WORK_BEFORE_BREAK1) {
            compliantClockOut = compliantClockOut.plus(Duration.ofMinutes(30))
        }

        return CompliantWorkingTime(
            originalClockIn = workingTimeResult.firstClockIn,
            originalClockOut = workingTimeResult.lastClockOut,
            originalTotalWorkingTime = workingTimeResult.totalWorkingTime,
            compliantClockIn = workingTimeResult.firstClockIn,
            compliantClockOut = compliantClockOut,
            compliantTotalWorkingTime = workingTimeResult.totalWorkingTime,
            workingTimeDiff = Duration.ZERO,
        )
    }
}

data class WorkingTimeResult(
    val firstClockIn: LocalTime?,
    val lastClockOut: LocalTime?,
    val totalWorkingTime: Duration,
    val totalBreakTime: Duration,
)

data class CompliantWorkingTime(
    val originalClockIn: LocalTime?,
    val originalClockOut: LocalTime?,
    val originalTotalWorkingTime: Duration,
    val compliantClockIn: LocalTime?,
    val compliantClockOut: LocalTime?,
    val compliantTotalWorkingTime: Duration,
    val workingTimeDiff: Duration,
)