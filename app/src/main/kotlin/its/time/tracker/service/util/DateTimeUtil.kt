package its.time.tracker.service.util

import its.time.tracker.service.AbortException
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.time.temporal.Temporal
import java.time.temporal.WeekFields
import java.util.*

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
                val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN, Locale.getDefault())
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
            if (dateInput.isNullOrBlank()) return LocalDate.now()
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
            val firstDayOfYear = date.dayOfWeek

            val dayIncr = when(firstDayOfYear.value) {
                1 -> 0L
                2 -> 6L
                3 -> 5L
                4 -> 4L
                5 -> 3L
                6 -> 2L
                7 -> 1L
                else -> 0L
            }

            return date.plusDays(dayIncr)
        }

        private fun parseStringWithPattern(string: String, pattern: String, verbose: Boolean = true): Temporal? {
            val dateFormatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
                .withResolverStyle(ResolverStyle.STRICT)

            return try {
                when (pattern) {
                    WEEK_PATTERN -> {
                        LocalTime.parse(string, dateFormatter)
                    }
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

        fun isSameDay(dateTime1: LocalDateTime, dateTime2: LocalDate): Boolean {
            return temporalToString(dateTime1, DATE_PATTERN) == temporalToString(dateTime2, DATE_PATTERN)
        }

        fun isSameMonth(dateTime1: LocalDateTime, dateTime2: LocalDate): Boolean {
            return temporalToString(dateTime1, MONTH_PATTERN) == temporalToString(dateTime2, MONTH_PATTERN)
        }

        fun temporalToString(dateTime: Temporal, pattern: String = DATE_TIME_PATTERN): String {
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
                .withZone(ZoneId.systemDefault())

            return formatter.format(dateTime)
        }

        fun durationToString(duration: Duration): String {
            return (if (duration.toHours() < 10) "0" else "") + duration.toHours() + ":" +
                    (if (duration.toMinutesPart() < 10) "0" else "") + duration.toMinutesPart()
        }

        fun getWeekOfYearFromDate(date: LocalDate): String {
            val weekFields: WeekFields = WeekFields.of(Locale.getDefault())
            val weekNumber: Int = date.get(weekFields.weekOfWeekBasedYear())

            return (if (weekNumber < 10) "0" else "") + weekNumber
        }
    }
}