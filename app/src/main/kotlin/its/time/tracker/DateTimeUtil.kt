package its.time.tracker

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.*


class DateTimeUtil {
    companion object {
        fun getNow(pattern: String = CLOCK_EVENT_PATTERN_FORMAT) : String {
            val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)
                .withZone(ZoneId.systemDefault())

            return formatter.format(Instant.now())
        }

        fun toValidDate(dateInput: String?): String? {
            if (dateInput.isNullOrBlank()) return getNow(DAY_PATTERN_FORMAT)
            else if (isValidDate(dateInput)) {
                return dateInput
            }

            System.err.println("invalid date input '$dateInput'")
            return null
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

        fun addTimes(time1: String, time2: String): String {
            val hours1: Int = time1.subSequence(0, 2).toString().toInt()
            val minutes1: Int = time1.subSequence(2, 4).toString().toInt()
            val hours2: Int = time2.subSequence(0, 2).toString().toInt()
            val minutes2: Int = time2.subSequence(2, 4).toString().toInt()

            var minutesSum = minutes1 + minutes2
            val hoursSum = Integer.sum(hours1 + hours2, if (minutesSum > 59) 1 else 0)
            minutesSum = if(minutesSum > 59) (minutesSum-60) else minutesSum

            return to4CharTimeFormat(hoursSum, minutesSum)
        }

        fun getTimeDiff(time1: String, time2: String): String {
            val hours1: Int = time1.subSequence(0, 2).toString().toInt()
            val minutes1: Int = time1.subSequence(2, 4).toString().toInt()
            var hours2: Int = time2.subSequence(0, 2).toString().toInt()
            var minutes2: Int = time2.subSequence(2, 4).toString().toInt()

            var hoursDecr = 0
            if (minutes1 > minutes2) {
                minutes2+=60
                hoursDecr = 1
            }
            val minutesDiff = minutes2 - minutes1

            if (hours1 > hours2) {
                hours2+=24
            }
            val hoursDiff = hours2 - hours1 - hoursDecr

            return to4CharTimeFormat(hoursDiff, minutesDiff)
        }

        private fun to4CharTimeFormat(hours: Int, minutes: Int): String {
            val hoursString: String = if(hours > 9) hours.toString() else "0$hours"
            val minutesString: String = if(minutes > 9) minutes.toString() else "0$minutes"

            return "$hoursString$minutesString"
        }
    }
}