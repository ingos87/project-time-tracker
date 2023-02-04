package its.time.tracker.upload

import its.time.tracker.config.Constants
import its.time.tracker.config.printDebug
import its.time.tracker.domain.CostAssessmentPosition
import its.time.tracker.util.DateTimeUtil
import its.time.tracker.webpages.WebElementService
import its.time.tracker.webpages.etime.ETimeAssessmentPage
import its.time.tracker.webpages.etime.ETimeLandingPage
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.*


class CostAssessmentUploader(private val costAssessmentsPerDay: SortedMap<LocalDate, List<CostAssessmentPosition>>) {

    private val webElementService: WebElementService = WebElementService()

    fun submit() {
        navigateToTimeCorrectionLandingPage()

        val bookingPageKeys = costAssessmentsPerDay.keys.groupBy { DateTimeUtil.getFirstBookingDay(it) }

        printDebug("performing cost assessments for these weeks: $bookingPageKeys")

        val eTimeAssessmentPage = ETimeAssessmentPage(webElementService)
        bookingPageKeys.forEach{ entry ->
            eTimeAssessmentPage.clickAllExpandIcons()
            eTimeAssessmentPage.selectWeek(entry.key)
            eTimeAssessmentPage.addAllTasksAndStandardActivities()

            val availableDaysForThisPage = entry.value
            availableDaysForThisPage.forEach { date ->
                ensureCostAssessmentValuesPresentForDate(date.dayOfWeek, costAssessmentsPerDay[date]!!)
            }

            eTimeAssessmentPage.clickSaveButton()
        }
    }

    private fun ensureCostAssessmentValuesPresentForDate(
        dayOfWeek: DayOfWeek,
        costAssessmentPositions: List<CostAssessmentPosition>,
    ) {
        val eTimeAssessmentPage = ETimeAssessmentPage(webElementService)
        val ids = eTimeAssessmentPage.getRelevantWebElementIds()

        val donePositions = mutableListOf<String>()
        ids.forEach {
            var cellContent = ""
            val costAssessmentPosition = costAssessmentPositions.find { pos -> pos.bookingKey == it.bookingPositionKey }
            if (costAssessmentPosition != null) {
                cellContent = DateTimeUtil.durationToDecimal(costAssessmentPosition.totalWorkingTime)
                donePositions.add(costAssessmentPosition.bookingKey)
            }
            eTimeAssessmentPage.insertHours(it.inputIdMap[dayOfWeek]!!, cellContent)
        }

        if (donePositions.size != costAssessmentPositions.size) {
            val missingPositionsOnPage = costAssessmentPositions.map { it.bookingKey }.minus(donePositions.toSet())
            println("Warning: unable to book hours for these cost assessment positions because they are not among your favorites: ${missingPositionsOnPage.joinToString()}")
            println("done:  $donePositions")
            println("input: $costAssessmentPositions")
        }
    }

    private fun navigateToTimeCorrectionLandingPage() {
        webElementService.navigateToUrl(Constants.E_TIME_URL)

        val eTimeLandingPage = ETimeLandingPage(webElementService)
        eTimeLandingPage.clickETimeTile(120L)

        val eTimeAssessmentPage = ETimeAssessmentPage(webElementService)
        eTimeAssessmentPage.doThingyToEnsurePageLoadingFinished()
    }
}