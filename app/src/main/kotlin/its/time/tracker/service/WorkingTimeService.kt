package its.time.tracker.service

import its.time.tracker.domain.WorkDaySummary
import its.time.tracker.service.util.*
import its.time.tracker.upload.WorkingTimeNormalizer
import its.time.tracker.upload.WorkingTimeUploader
import java.time.LocalDate

class WorkingTimeService {
    fun captureWorkingTime(localDate: LocalDate, granularity: Granularity, noop: Boolean) {
        val csvService = CsvService()
        val clockEvents = csvService.loadClockEvents()

        val allDays: List<LocalDate> = when(granularity) {
            Granularity.MONTH -> DateTimeUtil.getAllDaysInSameMonthAs(localDate)
            else -> DateTimeUtil.getAllDaysInSameWeekAs(localDate)
        }

        val workingTimeNormalizer = WorkingTimeNormalizer()
        val workingTimeResults = HashMap<LocalDate, WorkDaySummary>()
        allDays.forEach{ date ->
            val workDaySummary = WorkDaySummary.toWorkDaySummary(ClockEventsFilter.getEventsBelongingToSameDay(clockEvents, date))
            if (workDaySummary != null) {
                workingTimeResults[date] = workDaySummary
            }
        }

        val normalizedWorkingTimes = workingTimeNormalizer.normalizeWeekWorkingTime(workingTimeResults)

        println(" date       │ compliant values    ║ original values")
        println("────────────┼─────────────┼───────╬─────────────┼───────")

        normalizedWorkingTimes.forEach{ entry ->
            val sb = StringBuilder()
            sb.append(" " + DateTimeUtil.temporalToString(entry.key, DATE_PATTERN))
            sb.append(" │")
            sb.append(" " + DateTimeUtil.temporalToString(entry.value.last().clockIn.toLocalTime(), TIME_PATTERN))
            sb.append("-" + DateTimeUtil.temporalToString(entry.value.last().clockOut.toLocalTime(), TIME_PATTERN))
            sb.append(" │")
            sb.append(" " + DateTimeUtil.durationToString(entry.value.last().workDuration))
            sb.append(" ║")
            sb.append(" " + DateTimeUtil.temporalToString(entry.value.first().clockIn.toLocalTime(), TIME_PATTERN))
            sb.append("-" + DateTimeUtil.temporalToString(entry.value.first().clockOut.toLocalTime(), TIME_PATTERN))
            sb.append(" │")
            sb.append(" " + DateTimeUtil.durationToString(entry.value.first().workDuration))
            println(sb.toString())
        }

        if (noop) {
            println("\nNOOP mode. Uploaded nothing")
        } else {
            println("\nUploading clock-ins and clock-outs to myHRSelfService ...")
            val finalWorkingTimes = normalizedWorkingTimes.map { entry -> entry.key to entry.value.last() }.toMap()
            WorkingTimeUploader(finalWorkingTimes.toSortedMap()).submit()
        }
    }
}

enum class Granularity {
    WEEK, MONTH
}