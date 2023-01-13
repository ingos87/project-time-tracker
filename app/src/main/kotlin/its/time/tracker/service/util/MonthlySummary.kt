package its.time.tracker.service.util

import java.time.Duration
import java.time.LocalDate

data class MonthlySummary(
    var data: MutableMap<LocalDate, Pair<WorkingTimeResult, List<BookingPositionItem>>> = mutableMapOf()
) {
    fun addDay(dateTime: LocalDate,
               workingTimeResult: WorkingTimeResult,
               bookingPositionItems: List<BookingPositionItem>) {
        data[dateTime] = Pair(workingTimeResult, bookingPositionItems)
    }

    fun getAllClockIns(): List<String> {
        return data.values.map {
            if(it.first.firstClockIn == null) "flex" else DateTimeUtil.temporalToString(it.first.firstClockIn!!, TIME_PATTERN)
        }
    }

    fun getAllClockOuts(): List<String> {
        return data.values.map {
            if(it.first.lastClockOut == null) "flex" else DateTimeUtil.temporalToString(it.first.lastClockOut!!, TIME_PATTERN)
        }
    }

    fun getAllBookingPositionNames(): List<String> {
        return data.values.map { it.second.map { item -> item.bookingKey } }.flatten().distinct()
    }

    fun getAllTotalWorkingTimes(): List<String> {
        return data.values.map { DateTimeUtil.durationToString(it.first.totalWorkingTime) }
    }

    fun getAllBookingDurationsForKey(bookingKey: String): List<String> {
        var dateDurationsMap: Map<LocalDate, Duration> = HashMap()
        data.forEach{ (key, pair) ->
            val maybeBookingPositionItem = pair.second.find { item -> item.bookingKey == bookingKey }
            dateDurationsMap = dateDurationsMap.plus(Pair(key, maybeBookingPositionItem?.totalWorkingTime ?: Duration.ZERO))
        }

        return dateDurationsMap.values.map { if(it == Duration.ZERO) "     " else DateTimeUtil.durationToString(it) }
    }
}
