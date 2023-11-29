package project.time.tracker.upload

import project.time.tracker.domain.CostAssessmentPosition
import project.time.tracker.util.DateTimeUtil
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
                            roundingResult.first,
                            it.project,
                            it.topic,
                            it.story
                        )
                    )
                }
                roundingRemainders[it.getProjectKey()] = roundingRemainders.getOrDefault(it.getProjectKey(), Duration.ZERO) + roundingResult.second
            }
            roundedCostAssessments[date] = roundedBookingListItems
        }

        return Pair(roundedCostAssessments, roundingRemainders)
    }

    private fun addConsolidatedRemainders(
        costAssessments: MutableMap<LocalDate, List<CostAssessmentPosition>>,
        remainders: List<Pair<String, Duration>>
    ) {
        remainders.forEach { (projectKey, remainder) ->
            val key = getDateWithPresentProjectKey(costAssessments, projectKey) ?: costAssessments.keys.last()
            val bookingItemsList = costAssessments[key]
            costAssessments[key] = bookingItemsList!!.map {
                if (it.project == projectKey) {
                    CostAssessmentPosition(
                        it.totalWorkingTime + remainder,
                        it.project,
                        it.topic,
                        it.story
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

    private fun getDateWithPresentProjectKey(
        costAssessments: MutableMap<LocalDate, List<CostAssessmentPosition>>,
        projectKey: String
    ): LocalDate? {
        costAssessments.forEach { (date, list) ->
            val bookingItem = list.find { (it.getProjectKey()) == projectKey }
            if (bookingItem != null) {
                return date
            }
        }
        return null
    }
}