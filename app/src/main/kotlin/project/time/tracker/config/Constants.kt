package project.time.tracker.config

import project.time.tracker.config.Constants.Companion.VERBOSE
import project.time.tracker.domain.CostAssessmentProject
import project.time.tracker.domain.CostAssessmentSetup
import project.time.tracker.exception.AbortException
import project.time.tracker.util.DateTimeUtil
import java.time.*
import java.util.*

class Constants {
    companion object {
        const val COST_ASSMNT_DEV_KEY = "development_projects"
        const val COST_ASSMNT_MAINT_KEY = "maintenance_projects"
        const val COST_ASSMNT_INT_KEY = "internal_projects"
        const val COST_ASSMNT_ABSC_KEY = "absence_projects"

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
        val PUBLIC_HOLIDAYS: List<LocalDate> = listOf(
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
        var MY_HR_SELF_SERVICE_LANGUAGE: String = ""
        var E_TIME_URL: String = ""
        var E_TIME_LANGUAGE: String = ""
        var MAX_WORK_DURATION_TILL_AUTO_CLOCKOUT: Duration = Duration.ofHours(9)
        var STANDARD_WORK_DURATION_PER_DAY: Duration = Duration.ofHours(8)
        var WEEKDAYS_OFF: List<DayOfWeek> = emptyList()
        var DAYS_OFF: List<LocalDate> = emptyList()
        var SICK_LEAVE: List<Pair<LocalDate, LocalDate>> = emptyList()
        var CHILD_SICK_LEAVE: List<Pair<LocalDate, LocalDate>> = emptyList()
        var VACATION: List<Pair<LocalDate, LocalDate>> = emptyList()
        var CHROME_PROFILE_PATH: String = ""
        var COST_ASSESSMENT_SETUP: CostAssessmentSetup = CostAssessmentSetup.getEmptyInstance()

        fun setApplicationProperties(properties: Map<String, Any?>, verbose: Boolean) {
            VERBOSE = verbose
            CSV_PATH = readStringProperty(properties, Companion::CSV_PATH.name.lowercase(Locale.GERMANY))
            MY_HR_SELF_SERVICE_URL = readStringProperty(properties, Companion::MY_HR_SELF_SERVICE_URL.name.lowercase(Locale.GERMANY))
            MY_HR_SELF_SERVICE_LANGUAGE = readStringProperty(properties, Companion::MY_HR_SELF_SERVICE_LANGUAGE.name.lowercase(Locale.GERMANY))
            MAX_WORK_DURATION_TILL_AUTO_CLOCKOUT = Duration.parse(readStringProperty(properties, Companion::MAX_WORK_DURATION_TILL_AUTO_CLOCKOUT.name.lowercase(Locale.GERMANY)))
            STANDARD_WORK_DURATION_PER_DAY = Duration.parse(readStringProperty(properties, Companion::STANDARD_WORK_DURATION_PER_DAY.name.lowercase(Locale.GERMANY)))
            E_TIME_URL = readStringProperty(properties, Companion::E_TIME_URL.name.lowercase(Locale.GERMANY))
            E_TIME_LANGUAGE = readStringProperty(properties, Companion::E_TIME_LANGUAGE.name.lowercase(Locale.GERMANY))
            DAYS_OFF = parseDayList(properties[Companion::DAYS_OFF.name.lowercase(Locale.GERMANY)] as List<String>)
            SICK_LEAVE = parseDateSpans(properties[Companion::SICK_LEAVE.name.lowercase(Locale.GERMANY)] as List<String>)
            CHILD_SICK_LEAVE = parseDateSpans(properties[Companion::CHILD_SICK_LEAVE.name.lowercase(Locale.GERMANY)] as List<String>)
            VACATION = parseDateSpans(properties[Companion::VACATION.name.lowercase(Locale.GERMANY)] as List<String>)
            WEEKDAYS_OFF = parseWeekdayList(properties[Companion::WEEKDAYS_OFF.name.lowercase(Locale.GERMANY)] as String)
            CHROME_PROFILE_PATH = readStringProperty(properties, Companion::CHROME_PROFILE_PATH.name.lowercase(Locale.GERMANY))
            COST_ASSESSMENT_SETUP = readCostAssessmentMap(properties)
        }

        private fun parseDateSpans(listOfDateRanges: List<String>): List<Pair<LocalDate, LocalDate>> {
            return listOfDateRanges.mapNotNull { toPairOfDates(it) }
        }

        private fun toPairOfDates(range: String): Pair<LocalDate, LocalDate>? {
            val presumedDates = range.split("_")
            if (presumedDates.size == 2
                && presumedDates[0].length == 10
                && presumedDates[1].length == 10) {
                try {
                    return Pair(DateTimeUtil.toValidDate(presumedDates[0]) as LocalDate,
                        DateTimeUtil.toValidDate(presumedDates[1]) as LocalDate)
                } catch (e: AbortException) {
                    // ignore
                }
            }

            return null
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

        private fun parseDayList(daysOff: List<String>): List<LocalDate> {
            return daysOff.map { DateTimeUtil.toValidDate(it) as LocalDate }
        }

        private fun parseWeekdayList(daysOff: String?): List<DayOfWeek> {
            if (daysOff.isNullOrBlank()) {
                return emptyList()
            }
            return daysOff.split(",")
                .map { DayOfWeek.valueOf(it) }
        }

        private fun readCostAssessmentMap(map: Map<String, Any?>): CostAssessmentSetup {
            val topLevelKey = Companion::COST_ASSESSMENT_SETUP.name.lowercase(Locale.GERMANY)

            if (!map.containsKey(topLevelKey)) {
                return CostAssessmentSetup(emptyList(), emptyList(), emptyList(), emptyList())
            }

            val all = map[topLevelKey] as Map<*, *>
            return CostAssessmentSetup(
                developmentProjects = (all[COST_ASSMNT_DEV_KEY] as Map<String, String>)
                    .map { (k, v) -> CostAssessmentProject(k, v)},
                maintenanceProjects = (all[COST_ASSMNT_MAINT_KEY] as Map<String, String>)
                    .map { (k, v) -> CostAssessmentProject(k, v)},
                internalProjects = (all[COST_ASSMNT_INT_KEY] as Map<String, String>)
                    .map { (k, v) -> CostAssessmentProject(k, v)},
                absenceProjects = (all[COST_ASSMNT_ABSC_KEY] as Map<String, String>)
                    .map { (k, v) -> CostAssessmentProject(k, v)}
            )
        }
    }
}

fun printDebug(msg: String) {
    if (VERBOSE) {
        println("  DEBUG: ${LocalDateTime.now().toString().take(22).padEnd(22, ' ')}: $msg")
    }
}