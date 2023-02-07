package its.time.tracker.upload

import its.time.tracker.domain.CostAssessmentPosition
import its.time.tracker.util.DateTimeUtil
import java.time.Duration
import java.time.LocalDate
import java.util.*

class CostAssessmentRoundingService {

    fun roundProjectTimes(
        costAssessments: SortedMap<LocalDate, List<CostAssessmentPosition>>
    ): SortedMap<LocalDate, List<CostAssessmentPosition>> {
        val roundingResult = roundDurations(costAssessments)

        val roundingRemainders = roundingResult.second
        val roundedRoundingRemainders = roundIgnoringRemainders(roundingRemainders)

        val roundedCostAssessments = roundingResult.first
        addConsolidatedRemainders(roundedCostAssessments, roundedRoundingRemainders)

        return roundedCostAssessments.toSortedMap()
    }

    private fun roundDurations(
        costAssessments: SortedMap<LocalDate, List<CostAssessmentPosition>>
    ): Pair<MutableMap<LocalDate, List<CostAssessmentPosition>>, MutableMap<String, Duration>> {
        val roundedCostAssessments = mutableMapOf<LocalDate, List<CostAssessmentPosition>>()

        val roundingRemainders = mutableMapOf<String, Duration>()

        costAssessments.forEach { (date, value) ->
            val roundedBookingListItems = mutableListOf<CostAssessmentPosition>()
            value.forEach {
                val roundingResult = DateTimeUtil.roundToHalfHourWithRemainder(it.totalWorkingTime)
                if (roundingResult.first > Duration.ZERO) {
                    roundedBookingListItems.add(
                        CostAssessmentPosition(
                            it.project,
                            roundingResult.first,
                            it.topics
                        )
                    )
                }
                roundingRemainders[it.project] = roundingRemainders.getOrDefault(it.project, Duration.ZERO) + roundingResult.second
            }
            roundedCostAssessments[date] = roundedBookingListItems
        }

        return Pair(roundedCostAssessments, roundingRemainders)
    }

    private fun addConsolidatedRemainders(
        costAssessments: MutableMap<LocalDate, List<CostAssessmentPosition>>,
        remainders: List<Pair<String, Duration>>
    ) {
        remainders.forEach { (projectTitle, remainder) ->
            val key = getDateWithPresentProjectTitle(costAssessments, projectTitle) ?: costAssessments.keys.last()
            val bookingItemsList = costAssessments[key]
            costAssessments[key] = bookingItemsList!!.map {
                if (it.project == projectTitle) {
                    CostAssessmentPosition(
                        it.project,
                        it.totalWorkingTime + remainder,
                        it.topics
                    )
                } else {
                    it
                }
            }
        }
    }

    private fun roundIgnoringRemainders(roundingRemainders: MutableMap<String, Duration>) =
        roundingRemainders
            .map { (k, v) -> k to DateTimeUtil.roundToHalfHourWithRemainder(v).first }
            .filter { (_, v) -> v != Duration.ZERO }

    private fun getDateWithPresentProjectTitle(
        costAssessments: MutableMap<LocalDate, List<CostAssessmentPosition>>,
        projectTitle: String
    ): LocalDate? {
        costAssessments.forEach { (date, list) ->
            val bookingItem = list.find { it.project == projectTitle }
            if (bookingItem != null) {
                return date
            }
        }
        return null
    }
}