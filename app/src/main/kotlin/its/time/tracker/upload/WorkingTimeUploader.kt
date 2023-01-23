package its.time.tracker.upload

import its.time.tracker.config.Constants
import its.time.tracker.config.printDebug
import its.time.tracker.domain.WorkDaySummary
import its.time.tracker.util.DATE_PATTERN
import its.time.tracker.util.DateTimeUtil
import its.time.tracker.webpages.WebElementService
import its.time.tracker.webpages.myhrselfservice.MyHrSelfServiceLandingPage
import its.time.tracker.webpages.myhrselfservice.WorkingTimeCorrectionsPage
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*


class WorkingTimeUploader(private val workingTimesByDay: SortedMap<LocalDate, WorkDaySummary>) {

    private val webElementService: WebElementService = WebElementService()

    fun submit() {
        navigateToTimeCorrectionLandingPage()
        workingTimesByDay.forEach{ entry ->
            if (isWithinValidTimeframeForUpload(entry.key)) {
                navigateToDay(entry.key)
                ensureClockInClockOutPresent(entry.value)
            }
            else {
                println("unable to upload working time for ${DateTimeUtil.temporalToString(entry.key, DATE_PATTERN)} because it is longer than 30 days ago.")
            }
        }
    }

    private fun navigateToTimeCorrectionLandingPage() {
        webElementService.navigateToUrl(Constants.MY_HR_SELF_SERVICE_URL)

        val myHrSelfServiceLandingPage = MyHrSelfServiceLandingPage(webElementService)
        myHrSelfServiceLandingPage.clickTimeCorrectionsTile()

        val workingTimeCorrectionsPage = WorkingTimeCorrectionsPage(webElementService)
        workingTimeCorrectionsPage.doThingyToEnsurePageLoadingFinished()
    }

    private fun ensureClockInClockOutPresent(workDaySummary: WorkDaySummary?) {
        val timeCorrectionsPage = WorkingTimeCorrectionsPage(webElementService)
        val currentEventCount = timeCorrectionsPage.getCurrentEventCount()
        if (workDaySummary!!.workDuration == Duration.ZERO) {
            timeCorrectionsPage.clearAllEvents(currentEventCount)
            return
        }

        printDebug("found $currentEventCount clock events for ${DateTimeUtil.temporalToString(workDaySummary!!.clockIn.toLocalDate(), DATE_PATTERN)}")
        if (currentEventCount == 2) {
            val clockInTime = timeCorrectionsPage.getClockInTime()
            val clockOutTime = timeCorrectionsPage.getClockOutTime()
            if (workDaySummary.clockIn.toLocalTime().equals(clockInTime)
                && workDaySummary.clockOut.toLocalTime().equals(clockOutTime)) {
                return
            }
        }

        timeCorrectionsPage.clearAllEvents(currentEventCount)
        timeCorrectionsPage.createClockInAndCLockOut(workDaySummary)
    }

    private fun isWithinValidTimeframeForUpload(date: LocalDate): Boolean {
        val entryAge = ChronoUnit.DAYS.between(date, LocalDate.now())
        return entryAge <= 30L
    }

    private fun navigateToDay(date: LocalDate) {
        val timeCorrectionsPage = WorkingTimeCorrectionsPage(webElementService)
        timeCorrectionsPage.selectDate(date)
    }

}