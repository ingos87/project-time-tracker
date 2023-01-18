package its.time.tracker

import its.time.tracker.service.AbortException
import its.time.tracker.service.util.DateTimeUtil
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.util.*

class Constants {
    companion object {
        var VERBOSE: Boolean = false

        var CSV_PATH: String = ""
        var MY_HR_SELF_SERVICE_URL: String = ""
        var E_TIME_URL: String = ""
        var MAX_WORK_DURATION_PER_DAY: Duration = Duration.ofHours(9)
        var WEEKDAYS_OFF: List<DayOfWeek> = emptyList()
        var DAYS_OFF: List<LocalDate> = emptyList()

        fun setApplicationProperties(verbose: Boolean, properties: Map<String, Any?>) {
            VERBOSE = verbose
            CSV_PATH = readStringProperty(properties, ::CSV_PATH.name.lowercase(Locale.GERMANY))
            MY_HR_SELF_SERVICE_URL = readStringProperty(properties, ::MY_HR_SELF_SERVICE_URL.name.lowercase(Locale.GERMANY))
            MAX_WORK_DURATION_PER_DAY = Duration.parse(readStringProperty(properties, ::MAX_WORK_DURATION_PER_DAY.name.lowercase(Locale.GERMANY)))
            E_TIME_URL = readStringProperty(properties, ::E_TIME_URL.name.lowercase(Locale.GERMANY))
            DAYS_OFF = parseDayList(properties[::DAYS_OFF.name.lowercase(Locale.GERMANY)] as String)
            WEEKDAYS_OFF = parseWeekdayList(properties[::WEEKDAYS_OFF.name.lowercase(Locale.GERMANY)] as String)
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