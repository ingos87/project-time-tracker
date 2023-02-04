package its.time.tracker.webpages.myhrselfservice

import its.time.tracker.config.Constants
import its.time.tracker.config.printDebug
import its.time.tracker.domain.EventType
import its.time.tracker.domain.WorkDaySummary
import its.time.tracker.webpages.WebElementService
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class WorkingTimeCorrectionsPage(private val webElementService: WebElementService) {

    companion object {
        private const val DATE_INPUT_FIELD_ID           = "__xmlview2--CICO_DATE_PICKER-inner"

        private const val CURRENT_EVENTS_TITLE_DIV_ID   = "__xmlview2--CICO_PREVIOUS_EVENTS_FORM_CONTAINER--title"
        private const val ITEM_X_TIME_ID                = "__item4-__xmlview2--CICO_TIME_EVENT_LIST-%s-ObjectNumber"
        private const val ITEM_0_DEL_BUTTON_ID          = "__item4-__xmlview2--CICO_TIME_EVENT_LIST-0-imgDel"

        private const val EVENT_TYPE_INPUT_ID           = "__xmlview2--CICO_EVENT_TYPES-inner"
        private const val TIME_INPUT_ID                 = "__xmlview2--CICO_TIME-Picker-inner"

        fun getLocalizedConfirmButtonText(): String {
            return when(Constants.MY_HR_SELF_SERVICE_LANGUAGE) {
                "DE" -> "..." // TODO
                else -> "Confirm"
            }
        }

        fun getLocalizedCreateButtonText(): String {
            return when(Constants.MY_HR_SELF_SERVICE_LANGUAGE) {
                "DE" -> "..." // TODO
                else -> "Create"
            }
        }

        fun getLocalizedEventText(eventType: EventType): String {
            return when(eventType) {
                EventType.CLOCK_IN -> when(Constants.MY_HR_SELF_SERVICE_LANGUAGE) {
                    "DE" -> "..." // TODO
                    else -> "Homeoffice clockin"
                }
                EventType.CLOCK_OUT -> when(Constants.MY_HR_SELF_SERVICE_LANGUAGE) {
                    "DE" -> "..." // TODO
                    else -> "Homeoffice clockout"
                }
            }
        }
    }

    fun getCurrentEventCount(): Int {
        val text = webElementService.getElementTextualContent(CURRENT_EVENTS_TITLE_DIV_ID)
        if (!text.contains("(")) {
            return 0
        }
        return text.split("(")[1].substringBefore(")").toInt()
    }

    fun selectDate(date: LocalDate) {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.GERMANY)
            .withZone(ZoneId.systemDefault())
        val dateString = formatter.format(date)

        webElementService.setTextualContent(DATE_INPUT_FIELD_ID, dateString)
        doThingyToEnsurePageLoadingFinished()
    }

    fun clearAllEvents(maxListIdx: Int) {
        repeat(maxListIdx) {
            webElementService.clickOnElementWithId(ITEM_0_DEL_BUTTON_ID)
            webElementService.clickOnElementWithText(getLocalizedConfirmButtonText()) // modal dialog has rotating ids
        }
    }

    fun createClockInAndCLockOut(workDaySummary: WorkDaySummary?) {
        addClockEvent(EventType.CLOCK_IN, workDaySummary!!.clockIn.toLocalTime())
        addClockEvent(EventType.CLOCK_OUT, workDaySummary.clockOut.toLocalTime())
    }

    private fun addClockEvent(eventType: EventType, clockTime: LocalTime) {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY)
            .withZone(ZoneId.systemDefault())
        val timeString = formatter.format(clockTime)

        webElementService.setTextualContent(EVENT_TYPE_INPUT_ID, getLocalizedEventText(eventType))

        webElementService.setTextualContent(TIME_INPUT_ID, timeString.replace(":", ""))

        webElementService.clickOnElementWithText(getLocalizedCreateButtonText())

        webElementService.clickOnElementWithText(getLocalizedConfirmButtonText()) // modal dialog has rotating ids
    }

    fun getClockInTime(): LocalTime? {
        val timeString = webElementService.getElementTextualContent(String.format(ITEM_X_TIME_ID, 1))
        return LocalTime.parse(timeString)
    }

    fun getClockOutTime(): Any? {
        val timeString = webElementService.getElementTextualContent(String.format(ITEM_X_TIME_ID, 0))
        return LocalTime.parse(timeString)
    }

    fun doThingyToEnsurePageLoadingFinished() {
        webElementService.setTextualContent(EVENT_TYPE_INPUT_ID, "...")
        printDebug("WorkingTimeCorrectionsPage finished loading")
    }
}