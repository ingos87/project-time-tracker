package its.time.tracker.upload

import its.time.tracker.domain.WorkDaySummary
import java.time.LocalDate
import java.util.*

class WorkingTimeNormalizer {

    fun normalizeWorkingTime(workDaySummaries: Map<LocalDate, WorkDaySummary>): SortedMap<LocalDate, List<WorkDaySummary>> {
        val workDaySummariesAsList = HashMap<LocalDate, List<WorkDaySummary>>()
        workDaySummaries.forEach { entry ->
            workDaySummariesAsList[entry.key] = listOf(entry.value)
        }

        val srv = WorkingTimeDistributer()
        val correctlyDistributedWorkDaySummaries = srv.ensureMaxWorkingTimePerDay(workDaySummariesAsList)

        return srv.ensureRestPeriodBetweenDays(correctlyDistributedWorkDaySummaries)
    }
}