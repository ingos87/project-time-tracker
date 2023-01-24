package its.time.tracker.service

import its.time.tracker.domain.WorkDaySummary
import its.time.tracker.upload.CostAssessmentNormalizer
import its.time.tracker.util.ClockEventsFilter
import its.time.tracker.util.DateTimeUtil
import java.time.LocalDate
import java.util.SortedSet

class CostAssessmentService {

    fun captureProjectTimes(referenceDate: LocalDate, noop: Boolean) {
        val csvService = CsvService()
        val clockEvents = csvService.loadClockEvents()

        val allDays: SortedSet<LocalDate> = DateTimeUtil.getAllDaysInSameWeekAs(referenceDate)

        val workingTimeResults = HashMap<LocalDate, WorkDaySummary>()
        allDays.forEach{ date ->
            val workDaySummary = WorkDaySummary.toWorkDaySummary(ClockEventsFilter.getEventsBelongingToSameDay(clockEvents, date))
            if (workDaySummary != null) {
                workingTimeResults[date] = workDaySummary
            }
        }

        val normalizedWorkingTimes = CostAssessmentNormalizer().normalizeWorkingTime(workingTimeResults)
        // TODO show table


        if (noop) {
            println("\nNOOP mode. Uploaded nothing")
        } else {
            println("\nUploading clock-ins and clock-outs to myHRSelfService ...")
            // TODO implement
        }
    }
}