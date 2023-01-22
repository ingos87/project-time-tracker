package its.time.tracker.webpages.myhrselfservice

import its.time.tracker.domain.EventType
import its.time.tracker.domain.WorkDaySummary
import its.time.tracker.webpages.WebElementService
import org.openqa.selenium.Keys
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

const val ITEM_TIME_ID       = "__item4-__xmlview2--CICO_TIME_EVENT_LIST-%s-ObjectNumber"
const val ITEM_DEL_BUTTON_ID = "__item4-__xmlview2--CICO_TIME_EVENT_LIST-%s-imgDel"

class WorkingTimeCorrectionsPage(private val webElementService: WebElementService) {

    companion object {
        private const val DATE_INPUT_FIELD_ID = "__xmlview2--CICO_DATE_PICKER-inner"
        private const val CURRENT_EVENTS_TITLE_DIV_ID = "__xmlview2--CICO_PREVIOUS_EVENTS_FORM_CONTAINER--title"
        private const val EVENT_TYPE_INPUT_ID = "__xmlview2--CICO_EVENT_TYPES-inner"
        private const val TIME_TYPE_INPUT_ID = "__xmlview2--CICO_TIME-Picker-inner"
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
        webElementService.sendCharacter(DATE_INPUT_FIELD_ID, Keys.ENTER)

        webElementService.setTextualContent(EVENT_TYPE_INPUT_ID, "checking...")
    }

    fun clearAllEvents(maxListIdx: Int) {
        repeat(maxListIdx) {
            webElementService.clickOnElementWithId(String.format(ITEM_DEL_BUTTON_ID, 0))
            webElementService.clickOnElementWithText("Confirm")
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

        val eventTypeText = if (eventType==EventType.CLOCK_IN) "Homeoffice clockin" else "Homeoffice clockout"

        webElementService.setTextualContent(EVENT_TYPE_INPUT_ID, eventTypeText)

        webElementService.setTextualContent(TIME_TYPE_INPUT_ID, timeString.replace(":", ""))

        webElementService.clickOnElementWithText("Create")

        webElementService.clickOnElementWithText("Confirm")
    }

    fun getClockInTime(): LocalTime? {
        val timeString = webElementService.getElementTextualContent(String.format(ITEM_TIME_ID, 1))
        return LocalTime.parse(timeString)
    }

    fun getClockOutTime(): Any? {
        val timeString = webElementService.getElementTextualContent(String.format(ITEM_TIME_ID, 0))
        return LocalTime.parse(timeString)
    }
}