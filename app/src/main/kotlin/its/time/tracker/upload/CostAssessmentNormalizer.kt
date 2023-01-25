package its.time.tracker.upload

import its.time.tracker.domain.BookingPositionItem
import its.time.tracker.domain.WorkDaySummaryCollection
import its.time.tracker.util.DateTimeUtil
import java.time.Duration
import java.time.LocalDate
import java.util.*

class CostAssessmentNormalizer {

    fun normalizeWorkingTime(workDaySummaries: WorkDaySummaryCollection): SortedMap<LocalDate, List<BookingPositionItem>> {
        val compliantWorkDaySummaries = moveProjectTimesToValidDays(workDaySummaries)
        val roundedProjectTimes = roundProjectTimes(compliantWorkDaySummaries)

        return roundedProjectTimes.toSortedMap()
    }

    private fun roundProjectTimes(workDaySummaries: SortedMap<LocalDate, List<BookingPositionItem>>)
    : SortedMap<LocalDate, List<BookingPositionItem>> {
        val roundedSummaries = mutableMapOf<LocalDate, List<BookingPositionItem>>()

        val roundingRemainders = mutableMapOf<String, Duration>()

        workDaySummaries.forEach { (date, value) ->
            val roundedBookingListItems = mutableListOf<BookingPositionItem>()
            value.forEach {
                val roundingResult = DateTimeUtil.roundToHalfHourWithRemainder(it.totalWorkingTime)
                if (roundingResult.first > Duration.ZERO) {
                    roundedBookingListItems.add(
                        BookingPositionItem(
                            it.bookingKey,
                            roundingResult.first,
                            it.topics
                        )
                    )
                }
                roundingRemainders[it.bookingKey] = roundingRemainders.getOrDefault(it.bookingKey, Duration.ZERO) + roundingResult.second
            }
            roundedSummaries[date] = roundedBookingListItems
        }

        roundingRemainders
            .map { (k, v) -> k to DateTimeUtil.roundToHalfHourWithRemainder(v).first }
            .filter { (_, v) -> v != Duration.ZERO}

        roundingRemainders.forEach{ (bookingKey, remainder) ->
            val key = getDateWithPresentBookingKey(roundedSummaries, bookingKey)?:roundedSummaries.keys.last()
            val bookingItemsList = roundedSummaries[key]
            roundedSummaries[key] = bookingItemsList!!.map {
                if (it.bookingKey == bookingKey) {
                    BookingPositionItem(
                        it.bookingKey,
                        it.totalWorkingTime + remainder,
                        it.topics)
                } else {
                    it
                }
            }
        }

        return roundedSummaries.toSortedMap()
    }

    private fun getDateWithPresentBookingKey(
        summaries: MutableMap<LocalDate, List<BookingPositionItem>>,
        bookingKey: String
    ): LocalDate? {
        summaries.forEach { (date, list) ->
            val bookingItem = list.find { it.bookingKey == bookingKey }
            if (bookingItem != null) {
                return date
            }
        }
        return null
    }

    private fun moveProjectTimesToValidDays(workDaySummaries: WorkDaySummaryCollection): SortedMap<LocalDate, List<BookingPositionItem>> {
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