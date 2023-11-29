package project.time.tracker.upload

import project.time.tracker.config.Constants
import project.time.tracker.domain.CostAssessmentPosition
import project.time.tracker.util.DateTimeUtil
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.*

class CostAssessmentAbsenceService {

    private val PUBLIC_HOLIDAY = "Holidays"
    private val VACATION = "Holidays"
    private val SICK_LEAVE = "Sickness"
    private val OTHER = "Other absence"

    fun addAbsenceProjects(costAssessmentMap: Map<LocalDate, List<CostAssessmentPosition>>, uniqueDays: SortedSet<LocalDate>)
    : SortedMap<LocalDate, List<CostAssessmentPosition>> {

        val resultingMap = mutableMapOf<LocalDate, List<CostAssessmentPosition>>()
        resultingMap.putAll(costAssessmentMap)

        uniqueDays.forEach { date ->
            if (date.dayOfWeek != DayOfWeek.SUNDAY
                && date.dayOfWeek != DayOfWeek.SATURDAY
                && !costAssessmentMap.keys.contains(date)) {

                resultingMap[date] = listOf(
                    CostAssessmentPosition(
                        totalWorkingTime = Constants.STANDARD_WORK_DURATION_PER_DAY,
                        project = getAbsenceProject(date),
                        topic = "",
                        story = ""
                    )
                )
            }
        }

        return resultingMap.toSortedMap()
    }

    private fun getAbsenceProject(date: LocalDate): String {
        if (DateTimeUtil.isPublicHoliday(date)) {
            return PUBLIC_HOLIDAY
        }
        else if (DateTimeUtil.isVacationDay(date)) {
            return VACATION
        }
        else if (DateTimeUtil.isSickLeaveDay(date) || DateTimeUtil.isChildSickLeaveDay(date)) {
            return SICK_LEAVE
        }

        return OTHER
    }
}