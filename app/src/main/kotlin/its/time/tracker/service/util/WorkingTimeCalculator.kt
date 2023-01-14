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

    fun toWorkDaySummary(clockEvents: List<ClockEvent>, useNowAsCLockOut: Boolean = false): WorkDaySummary {
        if (clockEvents.isEmpty()) {
            return WorkDaySummary(
                clockIn = LocalTime.parse("00:00"),
                clockOut = LocalTime.parse("00:00"),
                workingTime = Duration.ZERO,
                breakTime = Duration.ZERO,
            )
        }

        val flexTimeEvent = clockEvents.find { it.eventType == EventType.FLEX_TIME }
        if (flexTimeEvent != null) {
            return WorkDaySummary(
                clockIn = null,
                clockOut = null,
                workingTime = Duration.ZERO,
                breakTime = Duration.ZERO,
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
            workingTime = totalWorkDuration,
            breakTime = totalBreakDuration
        )
    }

    fun normalizeWeekWorkingTime(workingTimeResults: HashMap<LocalDate, WorkDaySummary>): SortedMap<LocalDate, List<WorkDaySummary>> {
        val compliantWorkingTimeResults = HashMap<LocalDate, List<WorkDaySummary>>()
        workingTimeResults.forEach{ entry ->
            compliantWorkingTimeResults[entry.key] = listOf(entry.value, toCompliantWorkDaySummary(entry.value))
        }

        val correctyDistributedWorkingTimeResults = distributeWorkingTime(compliantWorkingTimeResults)

        return correctyDistributedWorkingTimeResults
    }

    fun distributeWorkingTime(complWorkingTimeResults: HashMap<LocalDate, List<WorkDaySummary>>): SortedMap<LocalDate, List<WorkDaySummary>> {
        var result = moveExtraTimeToNextDays(complWorkingTimeResults.toSortedMap(), Duration.ZERO)

        if (result.second > Duration.ZERO) {
            result = moveExtraTimeToNextDays(result.first.toSortedMap(Comparator.reverseOrder()), result.second)
        }

        return result.first.toSortedMap()
    }

    private fun moveExtraTimeToNextDays(
        complWorkingTimeResults: SortedMap<LocalDate, List<WorkDaySummary>>,
        additionalExtraTime: Duration
    ): Pair<SortedMap<LocalDate, List<WorkDaySummary>>, Duration> {

        val resultingWorkDaySummaryMap = HashMap<LocalDate, List<WorkDaySummary>>()
        var totalExtraTime = additionalExtraTime
        complWorkingTimeResults.forEach { entry ->
            val workDayV0 = entry.value[entry.value.size-2]
            val workDayV1 = entry.value.last()

            var newClockIn = workDayV1.clockIn
            var newClockOut = workDayV1.clockOut
            var newWorkingTime = workDayV1.workingTime
            var newBreakTime = workDayV1.breakTime

            val diffToMaxWorkingTime = MAX_WORK_PER_DAY - workDayV1.workingTime
            val currentExtraTime = workDayV0.workingTime - workDayV1.workingTime
            if (currentExtraTime > Duration.ZERO) {
                totalExtraTime += currentExtraTime
            } else if (totalExtraTime != Duration.ZERO && diffToMaxWorkingTime > Duration.ZERO) {
                if (totalExtraTime >= diffToMaxWorkingTime) {
                    // fill up to max
                    newWorkingTime += diffToMaxWorkingTime
                    val newClockWindow = addWorkingTimeToClockInAndOut(
                        workDayV1.clockIn!!,
                        workDayV1.clockOut!!,
                        diffToMaxWorkingTime
                    )
                    val correctlyMovedClockWindow = moveClockInAndClockOutToCompliantWindow(
                        newClockWindow.first,
                        newClockWindow.second
                    )
                    newClockIn = correctlyMovedClockWindow.first
                    newClockOut = correctlyMovedClockWindow.second
                    newBreakTime = newClockWindow.third

                    totalExtraTime -= diffToMaxWorkingTime
                } else {
                    // add all extra time
                    newWorkingTime += totalExtraTime
                    val newClockWindow = addWorkingTimeToClockInAndOut(
                        workDayV1.clockIn!!,
                        workDayV1.clockOut!!,
                        totalExtraTime
                    )
                    val correctlyMovedClockWindow = moveClockInAndClockOutToCompliantWindow(
                        newClockWindow.first,
                        newClockWindow.second
                    )
                    newClockIn = correctlyMovedClockWindow.first
                    newClockOut = correctlyMovedClockWindow.second
                    newBreakTime = newClockWindow.third

                    totalExtraTime = Duration.ZERO
                }
            }

            resultingWorkDaySummaryMap[entry.key] = entry.value + WorkDaySummary(
                clockIn = newClockIn,
                clockOut = newClockOut,
                workingTime = newWorkingTime,
                breakTime = newBreakTime,
            )
        }

        return Pair(resultingWorkDaySummaryMap.toSortedMap(), totalExtraTime)
    }

    fun addWorkingTimeToClockInAndOut(cIn: LocalTime, cOut: LocalTime, amount: Duration): Triple<LocalTime, LocalTime, Duration> {
        val breakTimeBefore = getTotalBreakDuration(Duration.between(cIn, cOut))
        var newClockOut = cOut + amount
        val breakTimeAfter = getTotalBreakDuration(Duration.between(cIn, newClockOut))

        newClockOut += (breakTimeAfter-breakTimeBefore)
        return Triple(cIn, newClockOut, breakTimeAfter)
    }

    fun getTotalBreakDuration(workingTime: Duration): Duration {
        if (workingTime <= MAX_WORK_BEFORE_BREAK1) {
            return Duration.ZERO
        }
        else if (workingTime <= MAX_WORK_BEFORE_BREAK2) {
            return Duration.ofMinutes(30)
        }
        return Duration.ofMinutes(45)
    }
    
    private fun moveClockInAndClockOutToCompliantWindow(cIn: LocalTime, cOut: LocalTime): Pair<LocalTime, LocalTime> {
        var clockIn = cIn
        var clockOut = cOut

        if (clockIn.isBefore(EARLIEST_START_OF_DAY)) {
            val postponeMinutes = MINUTES.between(clockIn, EARLIEST_START_OF_DAY)
            clockIn += Duration.ofMinutes(postponeMinutes)
            clockOut += Duration.ofMinutes(postponeMinutes)
        }
        if (clockOut.isAfter(LATEST_END_OF_DAY)) {
            val preponeMinutes = MINUTES.between(LATEST_END_OF_DAY, clockOut)
            clockIn -= Duration.ofMinutes(preponeMinutes)
            clockOut -= Duration.ofMinutes(preponeMinutes)
        }
        
        return Pair(clockIn, clockOut)
    }

    fun toCompliantWorkDaySummary(workDaySummary: WorkDaySummary): WorkDaySummary {
        // flex day
        if (workDaySummary.clockIn == null || workDaySummary.clockOut == null) {
            return workDaySummary
        }

        // add breaks
        var compliantClockOut = workDaySummary.clockIn + workDaySummary.workingTime
        val totalBreakDuration = getTotalBreakDuration(workDaySummary.workingTime)
        compliantClockOut += getTotalBreakDuration(workDaySummary.workingTime)

        // truncate if max hours is reached
        var compliantTotalWorkingTime = workDaySummary.workingTime
        if (workDaySummary.workingTime > MAX_WORK_PER_DAY) {
            compliantTotalWorkingTime = MAX_WORK_PER_DAY
            compliantClockOut -= workDaySummary.workingTime.minus(MAX_WORK_PER_DAY)
        }

        // move clock in and clock out into legal time window
        val clockInAndOutInLegalWindow = moveClockInAndClockOutToCompliantWindow(workDaySummary.clockIn, compliantClockOut)

        return WorkDaySummary(
            clockIn = clockInAndOutInLegalWindow.first,
            clockOut = clockInAndOutInLegalWindow.second,
            workingTime = compliantTotalWorkingTime,
            breakTime = totalBreakDuration
        )
    }
}

data class WorkDaySummary(
    val clockIn: LocalTime?,
    val clockOut: LocalTime?,
    val workingTime: Duration,
    val breakTime: Duration,
)