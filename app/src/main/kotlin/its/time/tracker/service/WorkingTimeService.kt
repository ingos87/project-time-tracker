package its.time.tracker.service

import its.time.tracker.service.util.DateTimeUtil
import its.time.tracker.service.util.WorkingTimeDistributionCalculator
import its.time.tracker.service.util.WorkDaySummary
import its.time.tracker.service.util.WorkingTimeCalculator
import java.time.LocalDate

class WorkingTimeService(
    private val verbose: Boolean,
    private val csvPath: String,
) {
    fun captureWorkingTime(localDate: LocalDate) {
        val csvService = CsvService(verbose, csvPath)
        val clockEvents = csvService.loadClockEvents()

        val allWeekDays: List<LocalDate> = DateTimeUtil.getAllDaysInSameWeekAs(localDate)

        val workingTimeCalculator = WorkingTimeCalculator()
        val workingTimeResults = HashMap<LocalDate, WorkDaySummary>()
        allWeekDays.forEach{date ->
            val workDaySummary = workingTimeCalculator.toWorkDaySummary(clockEvents.filter { event -> DateTimeUtil.isSameDay(event.dateTime, date)})
            if (workDaySummary != null) {
                workingTimeResults[date] = workDaySummary
            }
        }

        val normalizedWorkingtimes = workingTimeCalculator.normalizeWeekWorkingTime(workingTimeResults)


        println("done")
    }
}