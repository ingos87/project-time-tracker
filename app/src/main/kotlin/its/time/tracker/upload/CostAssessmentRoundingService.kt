package its.time.tracker.upload

import its.time.tracker.domain.BookingPositionItem
import its.time.tracker.util.DateTimeUtil
import java.time.Duration
import java.time.LocalDate
import java.util.*

class CostAssessmentRoundingService {

    fun roundProjectTimes(workDaySummaries: SortedMap<LocalDate, List<BookingPositionItem>>)
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

        val roundedRoundingRemainders = roundingRemainders
            .map { (k, v) -> k to DateTimeUtil.roundToHalfHourWithRemainder(v).first }
            .filter { (_, v) -> v != Duration.ZERO}

        roundedRoundingRemainders.forEach{ (bookingKey, remainder) ->
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
}