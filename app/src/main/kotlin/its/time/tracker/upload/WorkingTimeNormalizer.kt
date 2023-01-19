package its.time.tracker.upload

import its.time.tracker.domain.WorkDaySummary
import java.time.LocalDate
import java.util.HashMap
import java.util.SortedMap

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