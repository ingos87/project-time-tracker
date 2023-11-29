package project.time.tracker.domain

import project.time.tracker.util.DateTimeUtil
import project.time.tracker.util.TIME_PATTERN
import java.time.Duration
import java.time.LocalDate

data class WorkDaySummaryCollection(
    var data: MutableMap<LocalDate, Pair<WorkDaySummary, List<CostAssessmentPosition>>> = mutableMapOf()
) {
    fun addDay(dateTime: LocalDate,
               workDaySummary: WorkDaySummary,
               costAssessmentPositions: List<CostAssessmentPosition>) {
        data[dateTime] = Pair(workDaySummary, costAssessmentPositions)
    }

    fun getAllClockIns(): List<String> {
        return data.values.map {
            DateTimeUtil.temporalToString(it.first.clockIn, TIME_PATTERN)
        }
    }

    fun getAllClockOuts(): List<String> {
        return data.values.map {
            DateTimeUtil.temporalToString(it.first.clockOut, TIME_PATTERN)
        }
    }

    fun getAllBookingPositionNames(): List<String> {
        return data.values.map { it.second.map { item -> item.project } }.flatten().distinct()
    }

    fun getFilteredCostAssessmentPositionsBy(project: String?, topic: String?, story: String?):
            MutableMap<LocalDate, List<CostAssessmentPosition>> {
        val result: MutableMap<LocalDate, List<CostAssessmentPosition>> = mutableMapOf()

        data.forEach{ (key, pair) ->
            result[key] = pair.second.filter {
                (project == null || it.project == project)
                    && (topic == null || it.topic == topic)
                    && (story == null || it.story == story)
            }
        }

        return result
    }
}
