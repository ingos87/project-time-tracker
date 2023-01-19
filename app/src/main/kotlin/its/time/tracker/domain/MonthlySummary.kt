package its.time.tracker.domain

import its.time.tracker.util.DateTimeUtil
import its.time.tracker.util.TIME_PATTERN
import java.time.Duration
import java.time.LocalDate

data class MonthlySummary(
    var data: MutableMap<LocalDate, Pair<WorkDaySummary, List<BookingPositionItem>>> = mutableMapOf()
) {
    fun addDay(dateTime: LocalDate,
               workDaySummary: WorkDaySummary,
               bookingPositionItems: List<BookingPositionItem>) {
        data[dateTime] = Pair(workDaySummary, bookingPositionItems)
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
        return data.values.map { it.second.map { item -> item.bookingKey } }.flatten().distinct()
    }

    fun getAllTotalWorkingTimes(): List<String> {
        return data.values.map { DateTimeUtil.durationToString(it.first.workDuration) }
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
