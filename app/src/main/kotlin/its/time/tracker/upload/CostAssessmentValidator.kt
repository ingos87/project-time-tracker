package its.time.tracker.upload

import its.time.tracker.domain.CostAssessmentPosition
import its.time.tracker.util.DateTimeUtil
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
                resultingSummaries[date] = ProjectTimeCalculator.unifyPositions(bookingItemsSet)

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
            resultingSummaries[lastKey] = ProjectTimeCalculator.unifyPositions(bookingItemsSet)
        }

        return resultingSummaries.toSortedMap()
    }
}