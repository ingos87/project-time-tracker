package its.time.tracker.service.util

import its.time.tracker.MAX_WORK_HOURS_PER_DAY
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.min

data class WorkDaySummary(
    val clockIn: LocalTime,
    val clockOut: LocalTime,
    val workDuration: Duration,
    val breakDuration: Duration,
) {

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

        fun toWorkDaySummary(clockEvents: List<ClockEvent>, useNowAsCLockOut: Boolean = false): WorkDaySummary? {
            if (clockEvents.isEmpty()) {
                return null
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
                    totalWorkDuration += Duration.ofMinutes(ChronoUnit.MINUTES.between(mostRecentClockIn, now))
                    mostRecentClockOut = now
                }
                else if (totalWorkDuration.toHours() >= MAX_WORK_HOURS_PER_DAY) {
                    // although, this is beyond the max hours per day, any new tasks will take at least half an hour
                    totalWorkDuration += Duration.ofMinutes(30)
                    mostRecentClockOut = mostRecentClockIn!!.plusMinutes(30)

                    println(
                        DateTimeUtil.temporalToString(
                            clockEvents[0].dateTime,
                            DATE_PATTERN
                        ) + ": No final clock-out found. Will insert one. Work time will be ${
                            DateTimeUtil.durationToString(
                                totalWorkDuration
                            )
                        } hours.")
                }
                else {
                    val minutesTillMax = MAX_WORK_HOURS_PER_DAY * 60 - totalWorkDuration.toMinutes()

                    totalWorkDuration = Duration.ofHours(MAX_WORK_HOURS_PER_DAY.toLong())
                    mostRecentClockOut = mostRecentClockIn!!.plusMinutes(minutesTillMax)
                    println(
                        DateTimeUtil.temporalToString(
                            clockEvents[0].dateTime,
                            DATE_PATTERN
                        ) + ": No final clock-out found. Will insert one to fill up working time to maximum (${
                            DateTimeUtil.durationToString(
                                totalWorkDuration
                            )
                        } hours).")
                }
            }

            return WorkDaySummary(
                clockIn = firstClockIn!!.toLocalTime(),
                clockOut = mostRecentClockOut!!.toLocalTime(),
                workDuration = totalWorkDuration,
                breakDuration = totalBreakDuration
            )
        }
    }

    fun addWorkingTime(amount: Duration): Pair<WorkDaySummary, Duration> {
        if (workDuration == MAX_WORK_PER_DAY) {
            return Pair(this, amount)
        }

        val oldBreakDuration = getTotalBreakDuration(workDuration)
        var newWorkDuration = workDuration + amount
        val newBreakDuration = getTotalBreakDuration(newWorkDuration)

        var newClockOut = clockOut + amount
        var extraTime = Duration.ZERO
        if (newWorkDuration > MAX_WORK_PER_DAY) {
            extraTime = newWorkDuration - MAX_WORK_PER_DAY
            newWorkDuration = MAX_WORK_PER_DAY
            newClockOut -= extraTime
        }

        newClockOut += (newBreakDuration-oldBreakDuration)
        return Pair(WorkDaySummary(
            clockIn = clockIn,
            clockOut = newClockOut,
            workDuration = newWorkDuration,
            breakDuration = newBreakDuration
        ), extraTime)
    }

    fun makeCompliant(): Pair<WorkDaySummary, Duration> {
        // truncate if max hours is reached
        var compliantClockOut = clockIn + workDuration
        var compliantWorkDuration = workDuration
        var extraTime = Duration.ZERO
        if (workDuration > MAX_WORK_PER_DAY) {
            compliantWorkDuration = MAX_WORK_PER_DAY
            compliantClockOut -= workDuration.minus(MAX_WORK_PER_DAY)
            extraTime = workDuration - MAX_WORK_PER_DAY
        }

        // add breaks
        val totalBreakDuration = getTotalBreakDuration(workDuration)
        compliantClockOut += totalBreakDuration
        val workingTimeCompliantSummary = WorkDaySummary(
            clockIn = clockIn,
            clockOut = compliantClockOut,
            workDuration = compliantWorkDuration,
            breakDuration = totalBreakDuration
        )

        // move clock in and clock out into legal time window
        val compliantWorkDaySummary = workingTimeCompliantSummary.moveToComplyWithEarliestInLatestOut(
            EARLIEST_START_OF_DAY, LATEST_END_OF_DAY)

        return Pair(compliantWorkDaySummary, extraTime)
    }

    private fun moveToComplyWithEarliestInLatestOut(earliestIn: LocalTime?, latestOut: LocalTime?): WorkDaySummary {
        var newClockIn = clockIn
        var newClockOut = clockOut

        if (earliestIn != null && newClockIn.isBefore(earliestIn)) {
            val postponeMinutes = ChronoUnit.MINUTES.between(newClockIn, earliestIn)
            newClockIn += Duration.ofMinutes(postponeMinutes)
            newClockOut += Duration.ofMinutes(postponeMinutes)
        }
        if (latestOut != null && newClockOut.isAfter(latestOut)) {
            val preponeMinutes = ChronoUnit.MINUTES.between(latestOut, newClockOut)
            newClockIn -= Duration.ofMinutes(preponeMinutes)
            newClockOut -= Duration.ofMinutes(preponeMinutes)
        }

        return WorkDaySummary(
            clockIn = newClockIn,
            clockOut = newClockOut,
            workDuration = workDuration,
            breakDuration = breakDuration
        )
    }

    fun prepone(amount: Duration): Pair<WorkDaySummary, Duration> {
        if (amount == Duration.ZERO) {
            return Pair(this, amount)
        }

        val finalPreponeAmount: Duration
        var remainingAmount = Duration.ZERO

        val possiblePreponeMinutes = ChronoUnit.MINUTES.between(EARLIEST_START_OF_DAY, clockIn)
        finalPreponeAmount = Duration.ofMinutes(min(amount.toMinutes(), possiblePreponeMinutes))

        if (amount.toMinutes() > possiblePreponeMinutes) {
            remainingAmount = amount - Duration.ofMinutes(possiblePreponeMinutes)
        }

        return Pair(WorkDaySummary(
            clockIn = clockIn - finalPreponeAmount,
            clockOut = clockOut - finalPreponeAmount,
            workDuration = workDuration,
            breakDuration = breakDuration
        ), remainingAmount)
    }

    fun postpone(amount: Duration): Pair<WorkDaySummary, Duration> {
        if (amount == Duration.ZERO) {
            return Pair(this, amount)
        }

        val finalPostponeAmount: Duration
        var remainingAmount = Duration.ZERO

        // postpone
        val possiblePostponeMinutes = ChronoUnit.MINUTES.between(clockOut, LATEST_END_OF_DAY)
        finalPostponeAmount = Duration.ofMinutes(min(amount.toMinutes(), possiblePostponeMinutes))

        if (amount.toMinutes() > possiblePostponeMinutes) {
            remainingAmount = amount - Duration.ofMinutes(possiblePostponeMinutes)
        }

        return Pair(WorkDaySummary(
            clockIn = clockIn + finalPostponeAmount,
            clockOut = clockOut + finalPostponeAmount,
            workDuration = workDuration,
            breakDuration = breakDuration
        ), remainingAmount)
    }
}