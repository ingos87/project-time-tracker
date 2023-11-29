package project.time.tracker.upload

import project.time.tracker.config.Constants
import project.time.tracker.config.printDebug
import project.time.tracker.domain.WorkDaySummary
import project.time.tracker.util.DATE_PATTERN
import project.time.tracker.util.DateTimeUtil
import project.time.tracker.webpages.WebElementService
import project.time.tracker.webpages.myhrselfservice.MyHrSelfServiceLandingPage
import project.time.tracker.webpages.myhrselfservice.WorkingTimeCorrectionsPage
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*


class WorkingTimeUploader(private val workingTimesByDay: SortedMap<LocalDate, WorkDaySummary>) {

    private val webElementService: WebElementService = WebElementService()

    private fun getValidity(entry: Map.Entry<LocalDate, WorkDaySummary>): WorkingDayValidity {
        val entryAge = ChronoUnit.DAYS.between(entry.key, LocalDate.now())
        if (entryAge > 30L) {
            return WorkingDayValidity.OVER_30_DAYS_AGO
        }

        if (entry.value.clockOut.isAfter(LocalDateTime.now())) {
            return WorkingDayValidity.FUTURE_CLOCK_OUT
        }

        return WorkingDayValidity.VALID
    }

    fun submit() {
        navigateToTimeCorrectionLandingPage()
        workingTimesByDay.forEach{ entry ->
            when(getValidity(entry)) {
                WorkingDayValidity.OVER_30_DAYS_AGO -> {
                    println("unable to upload working time for ${DateTimeUtil.temporalToString(entry.key, DATE_PATTERN)} because it is longer than 30 days ago.")
                }
                WorkingDayValidity.FUTURE_CLOCK_OUT -> {
                    println("unable to upload working time for ${DateTimeUtil.temporalToString(entry.key, DATE_PATTERN)} because clock-out is a future date.")
                }
                else -> {
                    navigateToDay(entry.key)
                    ensureClockInClockOutPresent(entry.value)
                }
            }
        }
    }

    private fun navigateToTimeCorrectionLandingPage() {
        webElementService.navigateToUrl(Constants.MY_HR_SELF_SERVICE_URL)

        val myHrSelfServiceLandingPage = MyHrSelfServiceLandingPage(webElementService)
        myHrSelfServiceLandingPage.ensureAllModalsAreClosed()
        myHrSelfServiceLandingPage.clickTimeCorrectionsTile()

        val workingTimeCorrectionsPage = WorkingTimeCorrectionsPage(webElementService)
        workingTimeCorrectionsPage.recalibrateElementIds()
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

    private fun navigateToDay(date: LocalDate) {
        val timeCorrectionsPage = WorkingTimeCorrectionsPage(webElementService)
        timeCorrectionsPage.recalibrateElementIds()
        timeCorrectionsPage.selectDate(date)
    }

}

enum class WorkingDayValidity {
    VALID, OVER_30_DAYS_AGO, FUTURE_CLOCK_OUT
}