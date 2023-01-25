package its.time.tracker.upload

import its.time.tracker.domain.BookingPositionItem
import its.time.tracker.domain.WorkDaySummaryCollection
import its.time.tracker.util.DateTimeUtil
import java.time.LocalDate
import java.util.*

class CostAssessmentValidator {

    fun moveProjectTimesToValidDays(workDaySummaries: WorkDaySummaryCollection): SortedMap<LocalDate, List<BookingPositionItem>> {
        val resultingSummaries = mutableMapOf<LocalDate, List<BookingPositionItem>>()

        val carryOverBookingItems = mutableListOf<BookingPositionItem>()
        workDaySummaries.data.forEach{ (date, value) ->
            if (DateTimeUtil.isWorkingDay(date)) {
                val bookingItemsSet = mutableListOf<BookingPositionItem>()
                bookingItemsSet.addAll(value.second)
                bookingItemsSet.addAll(carryOverBookingItems)
                resultingSummaries[date] = bookingItemsSet.toList()

                carryOverBookingItems.clear()
            }
            else {
                carryOverBookingItems.addAll(value.second)
            }
        }

        if (carryOverBookingItems.isNotEmpty()) {
            val lastKey = resultingSummaries.keys.last()
            val bookingItemsSet = mutableListOf<BookingPositionItem>()
            bookingItemsSet.addAll(resultingSummaries[lastKey]!!)
            bookingItemsSet.addAll(carryOverBookingItems)
            resultingSummaries[lastKey] = bookingItemsSet.toList()
        }

        return resultingSummaries.toSortedMap()
    }
}