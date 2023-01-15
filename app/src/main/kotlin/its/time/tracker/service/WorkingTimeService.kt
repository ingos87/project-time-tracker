package its.time.tracker.service

import its.time.tracker.service.util.*
import java.time.LocalDate

class WorkingTimeService(
    private val verbose: Boolean,
    private val csvPath: String,
) {
    fun captureWorkingTime(localDate: LocalDate, noop: Boolean) {
        val csvService = CsvService(verbose, csvPath)
        val clockEvents = csvService.loadClockEvents()

        val allWeekDays: List<LocalDate> = DateTimeUtil.getAllDaysInSameWeekAs(localDate)

        val workingTimeNormalizer = WorkingTimeNormalizer()
        val workingTimeResults = HashMap<LocalDate, WorkDaySummary>()
        allWeekDays.forEach{date ->
            val workDaySummary = WorkDaySummary.toWorkDaySummary(clockEvents.filter { event -> DateTimeUtil.isSameDay(event.dateTime, date)})
            if (workDaySummary != null) {
                workingTimeResults[date] = workDaySummary
            }
        }

        // TODO regard working time on non-working days (must be moved to working days)
        // TODO enable possibility to work overnight
        val normalizedWorkingtimes = workingTimeNormalizer.normalizeWeekWorkingTime(workingTimeResults)

        println(" date       │ compliant values    ║ original values")
        println("────────────┼─────────────┼───────╬─────────────┼───────")

        normalizedWorkingtimes.forEach{ entry ->
            val sb = StringBuilder()
            if (entry.value.first().clockIn != null) {
                sb.append(" " + DateTimeUtil.temporalToString(entry.key, DATE_PATTERN))
                sb.append(" │")
                sb.append(" " + DateTimeUtil.temporalToString(entry.value.last().clockIn!!, TIME_PATTERN))
                sb.append("-" + DateTimeUtil.temporalToString(entry.value.last().clockOut!!, TIME_PATTERN))
                sb.append(" │")
                sb.append(" " + DateTimeUtil.durationToString(entry.value.last().workDuration))
                sb.append(" ║")
                sb.append(" " + DateTimeUtil.temporalToString(entry.value.first().clockIn!!, TIME_PATTERN))
                sb.append("-" + DateTimeUtil.temporalToString(entry.value.first().clockOut!!, TIME_PATTERN))
                sb.append(" │")
                sb.append(" " + DateTimeUtil.durationToString(entry.value.first().workDuration))
                println(sb.toString())
            }
            else {
                println(" ${DateTimeUtil.temporalToString(entry.key, DATE_PATTERN)} │ flex time day       ║ flex time day")
            }
        }

        if (!noop) {
            println("\nuploading clock-ins, clock-outs and flex time to myHRSelfService ...")
        }
    }
}