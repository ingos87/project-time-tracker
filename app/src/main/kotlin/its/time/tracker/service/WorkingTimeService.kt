package its.time.tracker.service

import its.time.tracker.service.util.*
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
            val workDaySummary = WorkDaySummary.toWorkDaySummary(clockEvents.filter { event -> DateTimeUtil.isSameDay(event.dateTime, date)})
            if (workDaySummary != null) {
                workingTimeResults[date] = workDaySummary
            }
        }

        // TODO regard working time on non-working days (must be moved to working days)
        // TODO enable possibility to work overnight
        val normalizedWorkingTimes = workingTimeNormalizer.normalizeWeekWorkingTime(workingTimeResults)

        println(" date       │ compliant values    ║ original values")
        println("────────────┼─────────────┼───────╬─────────────┼───────")

        normalizedWorkingTimes.forEach{ entry ->
            val sb = StringBuilder()
            sb.append(" " + DateTimeUtil.temporalToString(entry.key, DATE_PATTERN))
            sb.append(" │")
            sb.append(" " + DateTimeUtil.temporalToString(entry.value.last().clockIn, TIME_PATTERN))
            sb.append("-" + DateTimeUtil.temporalToString(entry.value.last().clockOut, TIME_PATTERN))
            sb.append(" │")
            sb.append(" " + DateTimeUtil.durationToString(entry.value.last().workDuration))
            sb.append(" ║")
            sb.append(" " + DateTimeUtil.temporalToString(entry.value.first().clockIn, TIME_PATTERN))
            sb.append("-" + DateTimeUtil.temporalToString(entry.value.first().clockOut, TIME_PATTERN))
            sb.append(" │")
            sb.append(" " + DateTimeUtil.durationToString(entry.value.first().workDuration))
            println(sb.toString())
        }

        if (!noop) {
            println("\nuploading clock-ins and clock-outs to myHRSelfService ...")
        }
    }
}

enum class Granularity {
    WEEK, MONTH
}