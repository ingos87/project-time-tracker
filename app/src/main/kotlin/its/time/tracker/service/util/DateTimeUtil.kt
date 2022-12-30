package its.time.tracker.service.util

import its.time.tracker.DATE_TIME_PATTERN
import its.time.tracker.DATE_PATTERN
import its.time.tracker.TIME_PATTERN
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit
import java.util.*

class DateTimeUtil {
    companion object {
        private fun getNow(pattern: String = DATE_TIME_PATTERN): String {
            val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)
                .withZone(ZoneId.systemDefault())

            return formatter.format(LocalDateTime.now())
        }

        fun toValidDate(dateInput: String?): String? {
            if (dateInput.isNullOrBlank()) return LocalDateTime.now()
            else if (isValidDate(dateInput)) {
                return dateInput
            }

            System.err.println("invalid date input '$dateInput'")
            return null
        }

        fun toValidDateTime(dateTimeInput: String?): String? {
            if (dateTimeInput.isNullOrBlank()) return getNow()
            else if (isValidTime(dateTimeInput)) {
                return getNow("yyyy-MM-dd") + "T" + dateTimeInput
            }
            else if (isValidDateTime(dateTimeInput)) {
                return dateTimeInput
            }

            System.err.println("invalid datetime input '$dateTimeInput'")
            return null
        }

        private fun isValidDateTime(dateTimeInput: String): Boolean {
            return checkDateTimeStringAgainstPattern(dateTimeInput, DATE_TIME_PATTERN)
        }

        fun isValidTime(time: String): Boolean {
            return checkDateTimeStringAgainstPattern(time, TIME_PATTERN)
        }

        fun isValidDate(date: String): Boolean {
            return checkDateTimeStringAgainstPattern(date, DATE_PATTERN)
        }

        private fun checkDateTimeStringAgainstPattern(string: String, pattern: String): LocalDateTime? {
            val dateFormatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
                .withResolverStyle(ResolverStyle.STRICT)

            return try {
                return dateFormatter.parse(string)
            } catch (e: DateTimeParseException) {
                System.err.println("unable to parse '$string' for pattern '$pattern'")
                return null
            }
        }

        fun extractTimeFromDateTime(dateTime: String): String {
            return dateTime.split("_")[1]
        }

        fun addTimeToDateTime(dateTime: String, time: String): String? {
            if (!isValidDateTime(dateTime) || !isValidTime(time)) {
                System.err.println("Unable to add time '$time' to datetime '$dateTime'")
                return null
            }

            val dateTimeSplit = dateTime.split("_")
            val newTime = addTimes(dateTimeSplit[1], time, true)
            if (newTime.toInt() >= dateTimeSplit[1].toInt()) {
                return "${dateTimeSplit[0]}_$newTime"
            }
            else {
                val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
                    .withZone(ZoneId.systemDefault())
                val localDateTime = LocalDateTime.parse(dateTimeSplit[0], formatter)
                localDateTime.plus(1, ChronoUnit.DAYS)

                return "${formatter.format(localDateTime)}_$newTime"
            }
        }

        fun addTimes(time1: String, time2: String, resetTimeAtNextDay: Boolean = false): String {
            val hours1: Int = time1.subSequence(0, 2).toString().toInt()
            val minutes1: Int = time1.subSequence(2, 4).toString().toInt()
            val hours2: Int = time2.subSequence(0, 2).toString().toInt()
            val minutes2: Int = time2.subSequence(2, 4).toString().toInt()

            var minutesSum = minutes1 + minutes2
            var hoursSum = Integer.sum(hours1 + hours2, if (minutesSum > 59) 1 else 0)
            minutesSum = if(minutesSum > 59) (minutesSum-60) else minutesSum
            if (resetTimeAtNextDay && hoursSum >= 24) hoursSum-=24

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