package its.time.tracker

import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.*


class DateTimeUtil {
    companion object {
        fun getNow(pattern: String = PATTERN_FORMAT) : String {
            val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)
                .withZone(ZoneId.systemDefault())

            return formatter.format(Instant.now())
        }

        fun toValidDateTime(dateTimeInput: String?): String? {
            if (dateTimeInput.isNullOrBlank()) return getNow()
            else if (isValidTime(dateTimeInput)) {
                return getNow("yyyyMMdd") + "_" + dateTimeInput
            }
            else if (isValidDateTime(dateTimeInput)) {
                return dateTimeInput
            }

            System.err.println("invalid datetime input '$dateTimeInput'")
            return null
        }

        private fun isValidDateTime(dateTimeInput: String): Boolean {
            if (dateTimeInput.length != 13 || dateTimeInput[8] != '_') {
                return false
            }
            val split = dateTimeInput.split("_")

            return split.size == 2 && isValidDate(split[0]) && isValidTime(split[1])
        }

        fun isValidTime(time: String): Boolean {
            val regex = "([01]?[0-9]|2[0-3])[0-5][0-9]".toRegex()
            return time.length == 4 && time.matches(regex)
        }

        fun isValidDate(date: String): Boolean {
            val dateFormatter = DateTimeFormatter.ofPattern("uuuuMMdd", Locale.getDefault())
                .withResolverStyle(ResolverStyle.STRICT)

            return try {
                dateFormatter.parse(date)
                true
            } catch (e: DateTimeParseException) {
                false
            }
        }
    }
}