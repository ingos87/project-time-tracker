package its.time.tracker.service.util

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.HashMap
import java.util.SortedMap
import java.util.TreeMap

val MIN_BREAK_BTW_DAYS: Duration = Duration.ofHours(11)

class WorkingTimeDistributionService {

    fun ensureRestPeriodBetweenDays(complWorkingTimeResults: SortedMap<LocalDate, List<WorkDaySummary>>): SortedMap<LocalDate, List<WorkDaySummary>> {
        var result : SortedMap<LocalDate, List<WorkDaySummary>> = complWorkingTimeResults

        var previousClockIns = listOf<LocalTime>(LocalTime.parse("08:15"))
        var currentClockIns = emptyList<LocalTime>()

        while (previousClockIns != currentClockIns) {
            previousClockIns = currentClockIns
            result = moveNextDaysWorkingTime(result)
            currentClockIns = getAllClockIns(result)
        }

        return result
    }

    private fun moveNextDaysWorkingTime(workDaySummariesMap: SortedMap<LocalDate, List<WorkDaySummary>>): SortedMap<LocalDate, List<WorkDaySummary>> {
        val originalMap = TreeMap<LocalDate, List<WorkDaySummary>>()
        workDaySummariesMap.map {
            entry -> originalMap[entry.key] = entry.value
        }

        val resultingWorkDaySummariesMap = HashMap<LocalDate, List<WorkDaySummary>>()
        var postponeDuration = Duration.ZERO

        originalMap.forEach { entry ->
            val currentWorkDaySummary = entry.value.last()

            val postponeResult = currentWorkDaySummary.postpone(postponeDuration)
            var currentWorkDaySummaryV2 = postponeResult.first
            postponeDuration = postponeResult.second
            val wasPostponed = currentWorkDaySummary != currentWorkDaySummaryV2

            val nextKey = originalMap.higherKey(entry.key)
            if (nextKey != null && originalMap.containsKey(nextKey)) {
                val nextWorkDaySummary = originalMap[nextKey]!!.last()

                val currentClockOut: LocalDateTime = entry.key.atTime(currentWorkDaySummaryV2.clockOut)
                val followingClockIn: LocalDateTime = nextKey.atTime(nextWorkDaySummary.clockIn)
                val interDayRestDuration = ChronoUnit.MINUTES.between(currentClockOut, followingClockIn)
                val restDeficitDuration = Duration.ofMinutes(MIN_BREAK_BTW_DAYS.toMinutes() - interDayRestDuration)

                if (restDeficitDuration > Duration.ZERO) {
                    if (wasPostponed) {
                        postponeDuration += restDeficitDuration
                    }
                    else {
                        val preponeResult = currentWorkDaySummaryV2.prepone(restDeficitDuration)
                        currentWorkDaySummaryV2 = preponeResult.first
                        if (preponeResult.second > Duration.ZERO) {
                            postponeDuration += preponeResult.second
                        }
                    }
                }

            }
            resultingWorkDaySummariesMap[entry.key] = entry.value + currentWorkDaySummaryV2
        }

        return resultingWorkDaySummariesMap.toSortedMap()
    }

    fun ensureMaxWorkingTimePerDay(complWorkingTimeResults: HashMap<LocalDate, List<WorkDaySummary>>): SortedMap<LocalDate, List<WorkDaySummary>> {
        var result = moveExtraTimeToAdjacentDays(complWorkingTimeResults.toSortedMap(), Duration.ZERO)

        if (result.second > Duration.ZERO) {
            result = moveExtraTimeToAdjacentDays(result.first.toSortedMap(Comparator.reverseOrder()), result.second)
        }

        return result.first.toSortedMap()
    }

    private fun moveExtraTimeToAdjacentDays(
        workDaySummariesMap: SortedMap<LocalDate, List<WorkDaySummary>>,
        additionalExtraTime: Duration
    ): Pair<SortedMap<LocalDate, List<WorkDaySummary>>, Duration> {

        val resultingWorkDaySummariesMap = HashMap<LocalDate, List<WorkDaySummary>>()
        var totalExtraTime = additionalExtraTime

        workDaySummariesMap.forEach { entry ->
            val currentWorkDaySummary = entry.value.last()

            val workDayWithAdditionalTime = currentWorkDaySummary.addWorkingTime(totalExtraTime)
            totalExtraTime = workDayWithAdditionalTime.second

            val complianceResult = workDayWithAdditionalTime.first.makeCompliant()
            // should not contain any more extra time

            resultingWorkDaySummariesMap[entry.key] = entry.value + complianceResult.first
        }

        return Pair(resultingWorkDaySummariesMap.toSortedMap(), totalExtraTime)
    }

    private fun getAllClockIns(map: SortedMap<LocalDate, List<WorkDaySummary>>): List<LocalTime> {
        return map.values
            .filter { it.last().clockIn != null }
            .map { it.last().clockIn!! }
    }
}