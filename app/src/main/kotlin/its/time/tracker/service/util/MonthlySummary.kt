package its.time.tracker.service.util

import java.time.Duration
import java.time.LocalDate

data class MonthlySummary(
    var data: MutableMap<LocalDate, Pair<WorkTimeResult, List<BookingPositionItem>>> = mutableMapOf()
) {
    fun addDay(dateTime: LocalDate,
               workTimeResult: WorkTimeResult,
               bookingPositionItems: List<BookingPositionItem>) {
        data[dateTime] = Pair(workTimeResult, bookingPositionItems)
    }

    fun getAllClockIns(): List<String> {
        return data.values.map { it.first.firstClockIn }
    }

    fun getAllClockOuts(): List<String> {
        return data.values.map { it.first.lastClockOut }
    }

    fun getAllBookingPositionNames(): List<String> {
        return data.values.map { it.second.map { item -> item.bookingKey } }.flatten()
    }

    fun getAllTotalWorkTimes(): List<String> {
        return data.values.map { it.first.totalWorkTime }
    }

    fun getAllBookingDurationsForKey(bookingKey: String): List<String> {
        var dateDurationsMap: Map<LocalDate, Duration> = HashMap()
        data.forEach{ (key, pair) ->
            val maybeBookingPositionItem = pair.second.find { item -> item.bookingKey == bookingKey }
            dateDurationsMap = dateDurationsMap.plus(Pair(key, maybeBookingPositionItem?.totalWorkTime ?: Duration.ZERO))
        }

        return dateDurationsMap.values.map { DateTimeUtil.durationToString(it) }
    }
}
