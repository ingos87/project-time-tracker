package its.time.tracker.upload

import its.time.tracker.domain.CostAssessmentPosition
import its.time.tracker.domain.WorkDaySummary
import its.time.tracker.domain.WorkDaySummaryCollection
import its.time.tracker.util.DateTimeUtil
import java.time.Duration
import java.time.LocalDate
import java.util.*

class CostAssessmentValidator {

    fun moveProjectTimesToValidDays(workDaySummaries: Map<LocalDate, List<CostAssessmentPosition>>)
    : SortedMap<LocalDate, List<CostAssessmentPosition>> {
        val resultingSummaries = mutableMapOf<LocalDate, List<CostAssessmentPosition>>()

        val carryOverBookingItems = mutableListOf<CostAssessmentPosition>()
        workDaySummaries.forEach{ (date, value) ->
            if (DateTimeUtil.isWorkingDay(date)) {
                val bookingItemsSet = mutableListOf<CostAssessmentPosition>()
                bookingItemsSet.addAll(value)
                bookingItemsSet.addAll(carryOverBookingItems)
                resultingSummaries[date] = combineByBookingId(bookingItemsSet)

                carryOverBookingItems.clear()
            }
            else {
                carryOverBookingItems.addAll(value)
            }
        }

        if (carryOverBookingItems.isNotEmpty()) {
            val lastKey = resultingSummaries.keys.last()
            val bookingItemsSet = mutableListOf<CostAssessmentPosition>()
            bookingItemsSet.addAll(resultingSummaries[lastKey]!!)
            bookingItemsSet.addAll(carryOverBookingItems)
            resultingSummaries[lastKey] = combineByBookingId(bookingItemsSet)
        }

        return resultingSummaries.toSortedMap()
    }

    private fun combineByBookingId(list: MutableList<CostAssessmentPosition>)
    : List<CostAssessmentPosition>{
        return list.groupBy{it.bookingKey}
            .map { (_, group) ->
                val minutesSum = group.sumOf { it.totalWorkingTime.toMinutes() }
                val allTopics = mutableSetOf<String>()
                group.forEach {
                    allTopics.addAll(it.topics)
                }
                CostAssessmentPosition(
                    group.first().bookingKey,
                    Duration.ofMinutes(minutesSum),
                    allTopics
                )
            }
    }
}