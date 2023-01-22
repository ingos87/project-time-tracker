package its.time.tracker.config

import its.time.tracker.config.Constants.Companion.VERBOSE
import its.time.tracker.exception.AbortException
import its.time.tracker.util.DateTimeUtil
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

class Constants {
    companion object {
        /**
         * MySelfHr/ArbSG rules
         * no work before 06:00
         * no work after 21:00
         * break between days: 11 hours
         * minimum break time after 6h work time: 30min
         * minimum break time after another 3h work time: 15min (45min in total)
         */
        val EARLIEST_START_OF_DAY: LocalTime = LocalTime.parse("06:00")
        val LATEST_END_OF_DAY: LocalTime = LocalTime.parse("21:00")
        val MAX_WORK_BEFORE_BREAK1: Duration = Duration.ofHours(6)
        val MAX_WORK_BEFORE_BREAK2: Duration = Duration.ofHours(9)
        val MAX_WORK_PER_DAY: Duration = Duration.ofHours(10)
        val MIN_BREAK_BTW_DAYS: Duration = Duration.ofHours(11)
        val WORK_FREE_DAYS: List<LocalDate> = listOf(
            "2023-01-01",
            "2023-03-08",
            "2023-04-07",
            "2023-04-10",
            "2023-05-01",
            "2023-05-18",
            "2023-05-29",
            "2023-10-03",
            "2023-12-24",
            "2023-12-25",
            "2023-12-26",
            "2023-12-31",
        ).map { DateTimeUtil.toValidDate(it) as LocalDate }

        var VERBOSE: Boolean = false

        var CSV_PATH: String = ""
        var MY_HR_SELF_SERVICE_URL: String = ""
        var E_TIME_URL: String = ""
        var MAX_WORK_DURATION_TILL_AUTO_CLOCKOUT: Duration = Duration.ofHours(9)
        var WEEKDAYS_OFF: List<DayOfWeek> = emptyList()
        var DAYS_OFF: List<LocalDate> = emptyList()

        fun setApplicationProperties(verbose: Boolean, properties: Map<String, Any?>) {
            VERBOSE = verbose
            CSV_PATH = readStringProperty(properties, Companion::CSV_PATH.name.lowercase(Locale.GERMANY))
            MY_HR_SELF_SERVICE_URL = readStringProperty(properties, Companion::MY_HR_SELF_SERVICE_URL.name.lowercase(Locale.GERMANY))
            MAX_WORK_DURATION_TILL_AUTO_CLOCKOUT = Duration.parse(readStringProperty(properties, Companion::MAX_WORK_DURATION_TILL_AUTO_CLOCKOUT.name.lowercase(Locale.GERMANY)))
            E_TIME_URL = readStringProperty(properties, Companion::E_TIME_URL.name.lowercase(Locale.GERMANY))
            DAYS_OFF = parseDayList(properties[Companion::DAYS_OFF.name.lowercase(Locale.GERMANY)] as String)
            WEEKDAYS_OFF = parseWeekdayList(properties[Companion::WEEKDAYS_OFF.name.lowercase(Locale.GERMANY)] as String)
        }

        private fun readStringProperty(map: Map<String, Any?>,
                                       fieldName: String,
                                       isOptional: Boolean = false,
                                       default: String = ""): String {
            if (!map.containsKey(fieldName)) {
                return if(isOptional) default else throw AbortException("Missing mandatory property '$fieldName'")
            }

            return map[fieldName] as String
        }

        private fun parseDayList(daysOff: String?): List<LocalDate> {
            if (daysOff.isNullOrBlank()) {
                return emptyList()
            }
            return daysOff.split(",")
                          .map { DateTimeUtil.toValidDate(it) as LocalDate }
        }

        private fun parseWeekdayList(daysOff: String?): List<DayOfWeek> {
            if (daysOff.isNullOrBlank()) {
                return emptyList()
            }
            return daysOff.split(",")
                .map { DayOfWeek.valueOf(it) }
        }
    }
}

fun printDebug(msg: String) {
    if (VERBOSE) println("  DEBUG: $msg")
}