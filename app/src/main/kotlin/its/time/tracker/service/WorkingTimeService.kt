package its.time.tracker.service

import its.time.tracker.service.util.DateTimeUtil
import its.time.tracker.service.util.WorkingTimeCalculator
import its.time.tracker.service.util.WorkingTimeResult
import java.time.LocalDate

class WorkingTimeService(
    private val verbose: Boolean,
    private val csvPath: String,
) {
    fun uploadWorkingTime(localDate: LocalDate) {
        val csvService = CsvService(verbose, csvPath)
        val clockEvents = csvService.loadClockEvents()

        val allWeekDays: List<LocalDate> = DateTimeUtil.getAllDaysInSameWeekAs(localDate)

        val workingTimeCalculator = WorkingTimeCalculator()
        val workingTimeResults = HashMap<LocalDate, WorkingTimeResult>()
        allWeekDays.forEach{date ->
            workingTimeResults[date] = workingTimeCalculator.calculateWorkingTime(clockEvents.filter { event -> DateTimeUtil.isSameDay(event.dateTime, date)})
        }

        //val normalizedWorkingtimes = workingTimeCalculator.normalizeWeekWorkingTime(workingTimeResults)


        println("done")
    }
}