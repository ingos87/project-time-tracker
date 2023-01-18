package its.time.tracker.service.util

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.HashMap
import java.util.SortedMap

/**
 * MySelfHr/ArbSG rules
 * no work before 06:00
 * no work after 21:00
 * break between days: 11 hours
 * minimum break time after 6h work time: 30min
 * minimum break time after another 3h work time: 15min (45min in total)
 */
val EARLIEST_START_OF_DAY: LocalTime = LocalTime.parse("06:00")
val LATEST_END_OF_DAY: LocalTime = LocalTime.parse("21:00")
val MAX_WORK_BEFORE_BREAK1: Duration = Duration.ofHours(6)
val MAX_WORK_BEFORE_BREAK2: Duration = Duration.ofHours(9)
val MAX_WORK_PER_DAY: Duration = Duration.ofHours(10)

class WorkingTimeNormalizer {

    fun normalizeWeekWorkingTime(workDaySummaries: Map<LocalDate, WorkDaySummary>): SortedMap<LocalDate, List<WorkDaySummary>> {
        val workDaySummariesAsList = HashMap<LocalDate, List<WorkDaySummary>>()
        workDaySummaries.forEach { entry ->
            workDaySummariesAsList[entry.key] = listOf(entry.value)
        }

        val srv = WorkingTimeDistributionService()
        val correctlyDistributedWorkDaySummaries = srv.ensureMaxWorkingTimePerDay(workDaySummariesAsList)

        return srv.ensureRestPeriodBetweenDays(correctlyDistributedWorkDaySummaries)
    }
}