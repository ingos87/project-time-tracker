package its.time.tracker.service.util

import java.time.Duration
import java.time.LocalDate
import java.util.HashMap
import java.util.SortedMap


class WorkingTimeDistributionCalculator {

    fun distributeWorkingTime(complWorkingTimeResults: HashMap<LocalDate, List<WorkDaySummary>>): SortedMap<LocalDate, List<WorkDaySummary>> {
        var result = moveExtraTimeToAdjacentDays(complWorkingTimeResults.toSortedMap(), Duration.ZERO)

        if (result.second > Duration.ZERO) {
            result = moveExtraTimeToAdjacentDays(result.first.toSortedMap(Comparator.reverseOrder()), result.second)
        }

        return result.first.toSortedMap()
    }

    private fun moveExtraTimeToAdjacentDays(
        complWorkingTimeResults: SortedMap<LocalDate, List<WorkDaySummary>>,
        additionalExtraTime: Duration
    ): Pair<SortedMap<LocalDate, List<WorkDaySummary>>, Duration> {

        val resultingWorkDaySummaryMap = HashMap<LocalDate, List<WorkDaySummary>>()
        var totalExtraTime = additionalExtraTime

        complWorkingTimeResults.forEach { entry ->
            val currentWorkDaySummary = entry.value.last()

            val workDayWithAdditionalTime = currentWorkDaySummary.addWorkingTime(totalExtraTime)
            totalExtraTime = workDayWithAdditionalTime.second

            val complianceResult = workDayWithAdditionalTime.first.makeCompliant()
            // should not contain any more extra time

            resultingWorkDaySummaryMap[entry.key] = entry.value + complianceResult.first
        }

        return Pair(resultingWorkDaySummaryMap.toSortedMap(), totalExtraTime)
    }
}