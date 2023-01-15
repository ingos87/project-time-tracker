package its.time.tracker.service.util

import java.time.Duration
import java.time.LocalTime
import java.time.temporal.ChronoUnit

data class WorkDaySummary(
    val clockIn: LocalTime?,
    val clockOut: LocalTime?,
    val workDuration: Duration,
    val breakDuration: Duration,
) {
    fun addWorkingTime(amount: Duration): Pair<WorkDaySummary, Duration> {
        if (workDuration == MAX_WORK_PER_DAY) {
            return Pair(this, amount)
        }

        val oldBreakDuration = WorkingTimeCalculator.getTotalBreakDuration(workDuration)
        var newWorkDuration = workDuration + amount
        val newBreakDuration = WorkingTimeCalculator.getTotalBreakDuration(newWorkDuration)

        var newClockOut = clockOut!! + amount
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
        // flex day
        if (clockIn == null || clockOut == null) {
            return Pair(this, Duration.ZERO)
        }

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
        val totalBreakDuration = WorkingTimeCalculator.getTotalBreakDuration(workDuration)
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
        if (clockIn == null || clockOut == null) {
            return this
        }

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

    private fun moveClockInAndClockOutToCompliantWindow(cIn: LocalTime, cOut: LocalTime): Pair<LocalTime, LocalTime> {
        var clockIn = cIn
        var clockOut = cOut

        if (clockIn.isBefore(EARLIEST_START_OF_DAY)) {
            val postponeMinutes = ChronoUnit.MINUTES.between(clockIn, EARLIEST_START_OF_DAY)
            clockIn += Duration.ofMinutes(postponeMinutes)
            clockOut += Duration.ofMinutes(postponeMinutes)
        }
        if (clockOut.isAfter(LATEST_END_OF_DAY)) {
            val preponeMinutes = ChronoUnit.MINUTES.between(LATEST_END_OF_DAY, clockOut)
            clockIn -= Duration.ofMinutes(preponeMinutes)
            clockOut -= Duration.ofMinutes(preponeMinutes)
        }

        return Pair(clockIn, clockOut)
    }
}