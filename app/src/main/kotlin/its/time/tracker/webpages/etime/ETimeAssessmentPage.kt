package its.time.tracker.webpages.etime

import its.time.tracker.config.Constants
import its.time.tracker.util.DateTimeUtil
import its.time.tracker.webpages.WebElementService
import org.openqa.selenium.By
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class ETimeAssessmentPage(private val webElementService: WebElementService) {

    companion object {
        private const val HELP_BUTTON_ID              = "helpButtonShell"

        fun getLocalizedAddFromFavoritesButtonText(): String {
            return when(Constants.E_TIME_LANGUAGE) {
                "DE" -> "..." // TODO
                else -> "Add from favorites"
            }
        }

        fun getLocalizedSaveButtonText(): String {
            return when(Constants.E_TIME_LANGUAGE) {
                "DE" -> "..." // TODO
                else -> "Save"
            }
        }
    }

    fun selectWeek(date: LocalDate) {
        val targetDayForSelection = DateTimeUtil.getFirstBookingDay(date)
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.GERMANY)
            .withZone(ZoneId.systemDefault())

        webElementService.clickOnElementWithText(formatter.format(targetDayForSelection))
        doThingyToEnsurePageLoadingFinished()
    }

    fun doThingyToEnsurePageLoadingFinished() {
        // save button should be clickable
        // else the page has not loaded yet OR that particular week is not editable
        webElementService.waitForElementToBeClickable(By.xpath("//*[text()='${getLocalizedSaveButtonText()}']"))
    }

    fun addAllTasksAndStandardActivities() {
        webElementService.clickOnElementWithText(getLocalizedAddFromFavoritesButtonText())
    }

    fun clickAllExpandIcons() {
        webElementService.clickOnAllElementWithTitle("Expand")
    }

    fun clickSaveButton() {
        webElementService.clickOnElementWithText(getLocalizedSaveButtonText())
    }
}