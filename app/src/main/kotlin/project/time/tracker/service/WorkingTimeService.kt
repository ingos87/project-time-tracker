package project.time.tracker.service

import project.time.tracker.domain.WorkDaySummary
import project.time.tracker.upload.WorkingTimeNormalizer
import project.time.tracker.upload.WorkingTimeUploader
import project.time.tracker.util.ClockEventsFilter
import project.time.tracker.util.DATE_PATTERN
import project.time.tracker.util.DateTimeUtil
import project.time.tracker.util.TIME_PATTERN
import java.time.LocalDate
import java.util.SortedSet

class WorkingTimeService {
    fun captureWorkingTime(referenceDate: LocalDate?, noop: Boolean) {
        val csvService = CsvService()
        val clockEvents = csvService.loadClockEvents()

        val allDays: SortedSet<LocalDate> =
            if (referenceDate == null) DateTimeUtil.getPrevious30Days(LocalDate.now())
            else DateTimeUtil.getAllDaysInSameWeekAs(referenceDate)

        val workingTimeResults = HashMap<LocalDate, WorkDaySummary>()
        allDays.forEach{ date ->
            val workDaySummary = WorkDaySummary.toWorkDaySummary(ClockEventsFilter.getEventsBelongingToSameDay(clockEvents, date))
            if (workDaySummary != null) {
                workingTimeResults[date] = workDaySummary
            }
        }

        val normalizedWorkingTimes = WorkingTimeNormalizer().normalizeWorkingTime(workingTimeResults)

        println(" date       │ compliant values            ║ original values")
        println("────────────┼─────────────┼───────────────╬─────────────┼───────")

        normalizedWorkingTimes.forEach{ entry ->
            val sb = StringBuilder()
            sb.append(" " + DateTimeUtil.temporalToString(entry.key, DATE_PATTERN))
            sb.append(" │")
            if (entry.value.last().clockIn == entry.value.last().clockOut) {
                sb.append("            ")
            } else {
                sb.append(" " + DateTimeUtil.temporalToString(entry.value.last().clockIn.toLocalTime(), TIME_PATTERN))
                sb.append("-" + DateTimeUtil.temporalToString(entry.value.last().clockOut.toLocalTime(), TIME_PATTERN))
            }
            sb.append(" │")
            sb.append(" " + DateTimeUtil.durationToString(entry.value.last().workDuration))
            sb.append(" (" + DateTimeUtil.durationToDecimal(entry.value.last().workDuration).padStart(5, ' '))
            sb.append(") ║")
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