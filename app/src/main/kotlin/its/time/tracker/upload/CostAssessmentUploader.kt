package its.time.tracker.upload

import its.time.tracker.config.Constants
import its.time.tracker.domain.CostAssessmentPosition
import its.time.tracker.util.DateTimeUtil
import its.time.tracker.webpages.WebElementService
import its.time.tracker.webpages.etime.ETimeAssessmentPage
import its.time.tracker.webpages.etime.ETimeLandingPage
import java.time.LocalDate
import java.util.*


class CostAssessmentUploader(private val costAssessmentsPerDay: SortedMap<LocalDate, List<CostAssessmentPosition>>) {

    private val webElementService: WebElementService = WebElementService()

    fun submit() {
        navigateToTimeCorrectionLandingPage()

        val bookingPageKeys = costAssessmentsPerDay.keys.groupBy { DateTimeUtil.getFirstBookingDay(it) }

        val eTimeAssessmentPage = ETimeAssessmentPage(webElementService)
        bookingPageKeys.forEach{ entry ->
            eTimeAssessmentPage.clickAllExpandIcons()
            eTimeAssessmentPage.selectWeek(entry.key)
            eTimeAssessmentPage.addAllTasksAndStandardActivities()

            // load all tasks and save in some variable
            //   save ids and table cell indicee to improve finding specific files
            entry.value.forEach { date ->
                ensureCostAssessmentValuesPresentForDate(date!!, costAssessmentsPerDay[date])
            }

            eTimeAssessmentPage.clickSaveButton()
        }
    }

    private fun ensureCostAssessmentValuesPresentForDate(
        date: LocalDate,
        costAssessmentPositions: List<CostAssessmentPosition>?
    ) {
        // plan:
        // identify correct column for this day
        // go through ALL cells of this day and check, whether work time has to be inserted
        // throw exception if booking position in assessmentObj is not present on website
        // walk over table, column-wise
    }

    private fun navigateToTimeCorrectionLandingPage() {
        webElementService.navigateToUrl(Constants.E_TIME_URL)

        val eTimeLandingPage = ETimeLandingPage(webElementService)
        eTimeLandingPage.clickETimeTile()

        val eTimeAssessmentPage = ETimeAssessmentPage(webElementService)
        eTimeAssessmentPage.doThingyToEnsurePageLoadingFinished()
    }
}