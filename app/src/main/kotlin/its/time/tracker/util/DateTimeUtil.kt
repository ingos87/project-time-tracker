package its.time.tracker.util

import its.time.tracker.config.Constants
import its.time.tracker.exception.AbortException
import java.time.*
import java.time.DayOfWeek.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.time.temporal.Temporal
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.*
import kotlin.math.roundToInt

const val DATE_TIME_PATTERN = "uuuu-MM-dd HH:mm"
const val DATE_PATTERN = "uuuu-MM-dd"
const val TIME_PATTERN = "HH:mm"
const val MONTH_PATTERN = "uuuu-MM"
const val WEEK_PATTERN = "uuuu-ww"

class DateTimeUtil {
    companion object {

        fun toValidDate(dateInput: String?): Temporal? {
            if (dateInput.isNullOrBlank()) return LocalDate.now()
            return parseStringWithPattern(dateInput, DATE_PATTERN)
        }

        fun toValidDateTime(dateTimeInput: String?): Temporal? {
            if (dateTimeInput.isNullOrBlank()) return LocalDateTime.now()

            try {
                val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN, Locale.GERMANY)
                    .withZone(ZoneId.systemDefault())
                return parseStringWithPattern(formatter.format(LocalDateTime.now()) + " " + dateTimeInput, DATE_TIME_PATTERN)
            } catch (e: AbortException) {
                // ignore
            }

            return parseStringWithPattern(dateTimeInput, DATE_TIME_PATTERN)
        }

        fun toValidMonth(dateInput: String?): Temporal? {
            if (dateInput.isNullOrBlank()) return LocalDate.now()
            val regex = "^\\d{4}-(0?[1-9]|1[012])$".toRegex()
            if (!dateInput.matches(regex)) {
                throw AbortException("unable to parse '$dateInput' for pattern '$MONTH_PATTERN'")
            }
            return parseStringWithPattern("$dateInput-01", DATE_PATTERN)
        }

        fun toValidCalendarWeek(dateInput: String?): Temporal? {
            if (dateInput.isNullOrBlank()) return null
            if (dateInput.contains("-") && dateInput.length == 7) {
                try {
                    val parts = dateInput.split("-")
                    if (parts.size == 2) {
                        val yearPart = Integer.parseInt(parts[0])
                        val weekNoPart = Integer.parseInt(parts[1])
                        if (weekNoPart in 1..53) {
                            val firstMondayOfYear = getFirstMondayOfYear(yearPart)
                            return firstMondayOfYear.plusWeeks(weekNoPart-1L)
                        }
                    }
                } catch (e: NumberFormatException) {
                    // swallow and exit as stated below
                }
            }
            throw AbortException("unable to parse '$dateInput' for pattern '$WEEK_PATTERN'")
        }

        private fun getFirstMondayOfYear(year: Int): LocalDate {
            val date = parseStringWithPattern("$year-01-01", DATE_PATTERN) as LocalDate
            val weekOfYear: Int = Integer.parseInt(getWeekOfYearFromDate(date))

            val firstDayOfYear = date.dayOfWeek

            val dayIncr = when(firstDayOfYear.value) {
                1 -> if(weekOfYear == 1) 0L else 7L
                2 -> if(weekOfYear == 1) -1L else 6L
                3 -> if(weekOfYear == 1) -2L else 5L
                4 -> if(weekOfYear == 1) -3L else 4L
                5 -> if(weekOfYear == 1) -4L else 3L
                6 -> if(weekOfYear == 1) -5L else 2L
                7 -> if(weekOfYear == 1) -6L else 1L
                else -> 0L
            }

            return date.plusDays(dayIncr)
        }

        private fun parseStringWithPattern(string: String, pattern: String): Temporal? {
            val dateFormatter = DateTimeFormatter.ofPattern(pattern, Locale.GERMANY)
                .withResolverStyle(ResolverStyle.STRICT)

            return try {
                when (pattern) {
                    TIME_PATTERN -> {
                        LocalTime.parse(string, dateFormatter)
                    }
                    DATE_PATTERN -> {
                        LocalDate.parse(string, dateFormatter)
                    }
                    else -> {
                        LocalDateTime.parse(string, dateFormatter)
                    }
                }
            } catch (e: DateTimeParseException) {
                throw AbortException("unable to parse '$string' for pattern '$pattern'")
            }
        }

        fun temporalToString(dateTime: Temporal, pattern: String = DATE_TIME_PATTERN): String {
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.GERMANY)
                .withZone(ZoneId.systemDefault())

            return formatter.format(dateTime)
        }

        fun durationToString(duration: Duration): String {
            return duration.toHours().toString().padStart(2, '0') + ":" +
                    duration.toMinutesPart().toString().padStart(2, '0')
        }

        fun durationToDecimal(duration: Duration): String {
            val minutesPartAsDecimal = ((duration.toMinutesPart() / 60.0) * 100.0).roundToInt().toString()
            return duration.toHours().toString() + "," + minutesPartAsDecimal.padStart(2, '0')
        }

        fun getWeekOfYearFromDate(date: LocalDate): String {
            val weekFields: WeekFields = WeekFields.of(Locale.GERMANY)
            val weekNumber: Int = date.get(weekFields.weekOfWeekBasedYear())

            return (if (weekNumber < 10) "0" else "") + weekNumber
        }

        fun getAllDaysInSameWeekAs(date: LocalDate): SortedSet<LocalDate> {
            val weekDayOrdinal: Long = date.dayOfWeek.ordinal.toLong()
            var plusValue = weekDayOrdinal * (-1)
            return listOf(
                date.plusDays(plusValue++),
                date.plusDays(plusValue++),
                date.plusDays(plusValue++),
                date.plusDays(plusValue++),
                date.plusDays(plusValue++),
                date.plusDays(plusValue++),
                date.plusDays(plusValue),
            ).toSortedSet()
        }

        fun getPrevious30Days(date: LocalDate): SortedSet<LocalDate> {
            val dateList = mutableListOf<LocalDate>()
            dateList.add(date)
            repeat(30) {
                dateList.add(dateList.last().minusDays(1))
            }

            return dateList.toSortedSet()
        }

        fun isWorkingDay(date: LocalDate): Boolean {
            val dayOfWeek = date.dayOfWeek
            return !Constants.WEEKDAYS_OFF.contains(dayOfWeek)
                && !Constants.PUBLIC_HOLIDAYS.contains(date)
                && !Constants.DAYS_OFF.contains(date)
        }

        fun roundToHalfHourWithRemainder(duration: Duration): Pair<Duration, Duration> {
            var halfHours = duration.toMinutes().toInt() / 30
            var remainder = duration.toMinutes().toInt() % 30

            if (remainder >= 15) {
                halfHours++
                remainder -= 30
            } else if (remainder < -15) {
                halfHours--
                remainder += 30
            }

            return Pair(Duration.ofMinutes((30 * halfHours).toLong()),
                Duration.ofMinutes(remainder.toLong()))
        }

        /**
         * returns monday of the given date's week
         * if that monday is situated in the previous month, the first day of the given date's month is returned
         * even though it is not a monday
         * if that first day of a month is on a weekend day, the monday before is returned
         */
        fun getFirstBookingDay(date: LocalDate): LocalDate {
            val startOfWeek = date.with(TemporalAdjusters.previousOrSame(MONDAY))
            return if (startOfWeek.month == date.month && startOfWeek.dayOfWeek != SATURDAY && startOfWeek.dayOfWeek != SUNDAY) {
                startOfWeek
            } else {
                val firstDayOfMonth = date.with(TemporalAdjusters.firstDayOfMonth())
                when (firstDayOfMonth.dayOfWeek) {
                    SATURDAY -> firstDayOfMonth.minusDays(5)
                    SUNDAY -> firstDayOfMonth.minusDays(6)
                    else -> firstDayOfMonth
                }
            }
        }
    }
}