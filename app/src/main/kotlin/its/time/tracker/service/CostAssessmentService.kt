package its.time.tracker.service

import its.time.tracker.domain.CostAssessmentPosition
import its.time.tracker.domain.WorkDaySummary
import its.time.tracker.domain.WorkDaySummaryCollection
import its.time.tracker.service.ConsoleTableHelper.Companion.getCellString
import its.time.tracker.service.ConsoleTableHelper.Companion.getContentLine
import its.time.tracker.service.ConsoleTableHelper.Companion.getHorizontalSeparator
import its.time.tracker.upload.CostAssessmentNormalizer
import its.time.tracker.upload.CostAssessmentUploader
import its.time.tracker.upload.ProjectTimeCalculator
import its.time.tracker.util.ClockEventsFilter
import its.time.tracker.util.DateTimeUtil
import java.time.Duration
import java.time.LocalDate
import java.util.*

class CostAssessmentService {

    fun captureProjectTimes(referenceDate: LocalDate, calculateForcast: Boolean, noop: Boolean, doSign: Boolean) {
        val csvService = CsvService()
        val clockEvents = csvService.loadClockEvents()

        val uniqueDays: SortedSet<LocalDate> = DateTimeUtil.getAllDaysInSameWeekAs(referenceDate)

        val summaryData = WorkDaySummaryCollection()
        uniqueDays.forEach { day ->
            val daysEvents = ClockEventsFilter.getEventsBelongingToSameDay(clockEvents, day)
            val workDaySummary = WorkDaySummary.toWorkDaySummary(daysEvents)
            val bookingPositionsList = ProjectTimeCalculator().calculateProjectTime(daysEvents)
            if (workDaySummary != null) {
                summaryData.addDay(day, workDaySummary, bookingPositionsList)
            }
        }
        if (summaryData.data.isEmpty()) {
            println("[NO SUMMARY for ${uniqueDays.first()} - ${uniqueDays.last()} because there are no clock-in events]")
            return
        }

        val costAssessmentMap = summaryData.data.map { (k, v) -> k to v.second }.toMap()
        val normalizedWorkingTimes = CostAssessmentNormalizer().normalizeWorkingTime(costAssessmentMap)

        val firstColWidth = 18 //BookingPositionResolver.getMaxBookingPosNameLength()+2
        println("[SUMMARY for ${uniqueDays.first()} - ${uniqueDays.last()}]")
        println(getHorizontalSeparator(uniqueDays, SeparatorPosition.TOP, firstColWidth, false))
        println(getContentLine(
            getCellString("weekday", firstColWidth, TextOrientation.LEFT),
            uniqueDays.map { it.dayOfWeek.name.substring(0, 3) },
            uniqueDays))
        println(getContentLine(
            getCellString("day of month", firstColWidth, TextOrientation.LEFT),
            uniqueDays.map { it.dayOfMonth.toString() },
            uniqueDays))
        println(getHorizontalSeparator(uniqueDays, SeparatorPosition.MIDDLE, firstColWidth, false))

        val allBookingPositionNames = summaryData.getAllBookingPositionNames()
        allBookingPositionNames.forEach { name ->
            println(getContentLine(
                getCellString(name, firstColWidth, TextOrientation.LEFT),
                getBookingTimesForProject(normalizedWorkingTimes, name, uniqueDays),
                uniqueDays))
        }

        println(getHorizontalSeparator(uniqueDays, SeparatorPosition.BOTTOM, firstColWidth, false))

        if (noop) {
            println("\nNOOP mode. Uploaded nothing")
        } else {
            println("\nUploading clock-ins and clock-outs to eTime ...")
            CostAssessmentUploader(normalizedWorkingTimes).submit(doSign)
        }
    }

    private fun getBookingTimesForProject(
        normalizedWorkingTimes: SortedMap<LocalDate, List<CostAssessmentPosition>>,
        name: String,
        uniqueDays: SortedSet<LocalDate>
    ): List<String> {
        val times = mutableListOf<String>()
        uniqueDays.forEach { date ->
            val projectDuration = normalizedWorkingTimes[date]?.find { it -> it.project == name }?.totalWorkingTime
            times.add(if (projectDuration == null || projectDuration == Duration.ZERO) "     "
                        else DateTimeUtil.durationToDecimal(projectDuration).padStart(5, ' '))
        }

        return times
    }
}