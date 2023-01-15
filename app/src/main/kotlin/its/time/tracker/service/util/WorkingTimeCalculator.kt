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
import java.util.SortedMap

/**
 * MySelfHr/ArbSG rules
 * no work before 06:00
 * no work after 21:00
 * break between days: 11 hours
 * minimum break time after 6h work time: 30min
 * minimum break time after another 3h work time: 15min (45min in total)
 */
val EARLIEST_START_OF_DAY: LocalTime = LocalTime.parse("06:00")
val LATEST_END_OF_DAY: LocalTime = LocalTime.parse("21:00")
val MIN_BREAK_BTW_DAYS: Duration = Duration.ofHours(11)
val MAX_WORK_BEFORE_BREAK1: Duration = Duration.ofHours(6)
val MAX_WORK_BEFORE_BREAK2: Duration = Duration.ofHours(9)
val MAX_WORK_PER_DAY: Duration = Duration.ofHours(10)

class WorkingTimeCalculator {

    companion object {
        fun getTotalBreakDuration(workingTime: Duration): Duration {
            if (workingTime <= MAX_WORK_BEFORE_BREAK1) {
                return Duration.ZERO
            }
            else if (workingTime <= MAX_WORK_BEFORE_BREAK2) {
                return Duration.ofMinutes(30)
            }
            return Duration.ofMinutes(45)
        }
    }

    fun toWorkDaySummary(clockEvents: List<ClockEvent>, useNowAsCLockOut: Boolean = false): WorkDaySummary? {
        if (clockEvents.isEmpty()) {
            return null
        }

        val flexTimeEvent = clockEvents.find { it.eventType == EventType.FLEX_TIME }
        if (flexTimeEvent != null) {
            return WorkDaySummary(
                clockIn = null,
                clockOut = null,
                workDuration = Duration.ZERO,
                breakDuration = Duration.ZERO,
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
                    totalBreakDuration += breakDuration
                    mostRecentClockIn = it.dateTime
                }

                currentClockStatus = EventType.CLOCK_IN
            }
            else if (it.eventType == EventType.CLOCK_OUT) {
                if (currentClockStatus == EventType.CLOCK_IN) {
                    val workDuration = Duration.between(mostRecentClockIn!!, it.dateTime)
                    totalWorkDuration += workDuration
                    mostRecentClockOut = it.dateTime
                }

                currentClockStatus = EventType.CLOCK_OUT
            }
        }

        if (currentClockStatus != EventType.CLOCK_OUT) {
            if (useNowAsCLockOut) {
                val now = LocalDateTime.now()
                totalWorkDuration += Duration.ofMinutes(MINUTES.between(mostRecentClockIn, now))
                mostRecentClockOut = now
            }
            else if (totalWorkDuration.toHours() >= MAX_WORK_HOURS_PER_DAY) {
                // although, this is beyond the max hours per day, any new tasks will take at least half an hour
                totalWorkDuration += Duration.ofMinutes(30)
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

        return WorkDaySummary(
            clockIn = firstClockIn?.toLocalTime(),
            clockOut = mostRecentClockOut?.toLocalTime(),
            workDuration = totalWorkDuration,
            breakDuration = totalBreakDuration
        )
    }

    fun normalizeWeekWorkingTime(workDaySummaries: HashMap<LocalDate, WorkDaySummary>): SortedMap<LocalDate, List<WorkDaySummary>> {
        val workDaySummariesAsList = HashMap<LocalDate, List<WorkDaySummary>>()
        workDaySummaries.forEach { entry ->
            workDaySummariesAsList[entry.key] = listOf(entry.value)
        }

        val correctlyDistributedWorkDaySummaries =
            WorkingTimeDistributionCalculator().distributeWorkingTime(workDaySummariesAsList)

        return spreadOutWorkDays(correctlyDistributedWorkDaySummaries)
    }

    private fun spreadOutWorkDays(workDaySummaries: SortedMap<LocalDate, List<WorkDaySummary>>): SortedMap<LocalDate, List<WorkDaySummary>> {
        // TODO implement
        return  workDaySummaries
    }

}