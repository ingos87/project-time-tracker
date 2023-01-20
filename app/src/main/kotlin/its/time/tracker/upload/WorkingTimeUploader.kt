package its.time.tracker.upload

import io.github.bonigarcia.wdm.WebDriverManager
import its.time.tracker.config.Constants
import its.time.tracker.domain.WorkDaySummary
import its.time.tracker.webpages.myhrselfservice.LandingPage
import its.time.tracker.webpages.myhrselfservice.WorkingTimeCorrectionsPage
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

class WorkingTimeUploader(private val workingTimesByDay: SortedMap<LocalDate, WorkDaySummary>) {

    private val webDriver = WebDriverManager.chromedriver().create()

    fun submit() {
        navigateToTimeCorrectionLandingPage()
        workingTimesByDay.forEach{ entry ->
            if (isValidInput(entry)) {
                navigateToDay(entry.key)
                ensureClockInClockOutPresent(entry.value)
                return // FIXME remove
            }
        }
    }

    private fun navigateToTimeCorrectionLandingPage() {
        webDriver.get(Constants.MY_HR_SELF_SERVICE_URL)

        val landingPage = LandingPage(webDriver)
        landingPage.clickTimeCorrectionsTile()
    }

    private fun ensureClockInClockOutPresent(workDaySummary: WorkDaySummary?) {
        val timeCorrectionsPage = WorkingTimeCorrectionsPage(webDriver)
        val currentEventCount = timeCorrectionsPage.getCurrentEventCount()

        if (currentEventCount == 2) {
            val clockInTime = timeCorrectionsPage.getClockInTime()
            val clockOutTime = timeCorrectionsPage.getClockOutTime()
            if (workDaySummary?.clockIn?.toLocalTime()?.equals(clockInTime)!!
                && workDaySummary.clockOut.toLocalTime().equals(clockOutTime)) {
                return
            }
        }

        timeCorrectionsPage.clearAllEvents(currentEventCount)
        timeCorrectionsPage.createClockInAndCLockOut(workDaySummary)
    }

    private fun isValidInput(entry: Map.Entry<LocalDate, WorkDaySummary>): Boolean {
        val entryAge = ChronoUnit.DAYS.between(entry.key, LocalDate.now())
        return entryAge <= 30L
    }

    private fun navigateToDay(date: LocalDate) {
        val timeCorrectionsPage = WorkingTimeCorrectionsPage(webDriver)
        timeCorrectionsPage.selectDate(date)
    }

}