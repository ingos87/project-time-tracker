package its.time.tracker.service.util

import its.time.tracker.DATE_TIME_PATTERN
import its.time.tracker.DATE_PATTERN
import its.time.tracker.TIME_PATTERN
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.util.*

class DateTimeUtil {
    companion object {

        fun toValidDate(dateInput: String?): Temporal? {
            if (dateInput.isNullOrBlank()) return LocalDateTime.now()
            return parseStringWithPattern(dateInput, DATE_PATTERN)
        }

        fun toValidDateTime(dateTimeInput: String?): Temporal? {
            if (dateTimeInput.isNullOrBlank()) return LocalDateTime.now()

            val time = parseStringWithPattern(dateTimeInput, TIME_PATTERN, false)
            if (time != null) {
                val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN, Locale.getDefault())
                    .withZone(ZoneId.systemDefault())
                return parseStringWithPattern(formatter.format(LocalDateTime.now()) + " " + dateTimeInput, DATE_TIME_PATTERN)
            }

            return parseStringWithPattern(dateTimeInput, DATE_TIME_PATTERN)
        }

        private fun parseStringWithPattern(string: String, pattern: String, verbose: Boolean = true): Temporal? {
            val dateFormatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
                .withResolverStyle(ResolverStyle.STRICT)

            return try {
                if (pattern == TIME_PATTERN) {
                    LocalTime.parse(string, dateFormatter)
                }
                else if (pattern == DATE_PATTERN) {
                    LocalDate.parse(string, dateFormatter)
                }
                else {
                    LocalDateTime.parse(string, dateFormatter)
                }
            } catch (e: DateTimeParseException) {
                if (verbose) System.err.println("unable to parse '$string' for pattern '$pattern'")
                null
            }
        }

        fun isSameDay(dateTime1: LocalDateTime, dateTime2: LocalDate): Boolean {
            return dateTimeToString(dateTime1, DATE_PATTERN) == dateTimeToString(dateTime2, DATE_PATTERN)
        }

        fun dateTimeToString(dateTime: Temporal, pattern: String = DATE_TIME_PATTERN): String {
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
                .withZone(ZoneId.systemDefault())

            return formatter.format(dateTime)
        }

        fun durationToString(duration: Duration): String {
            return (if (duration.toHours() < 10) "0" else "") + duration.toHours() + ":" +
                    (if (duration.toMinutesPart() < 10) "0" else "") + duration.toMinutesPart()
        }
    }
}