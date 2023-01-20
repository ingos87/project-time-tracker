package its.time.tracker.webpages.myhrselfservice

import its.time.tracker.domain.WorkDaySummary
import its.time.tracker.util.DATE_PATTERN
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.FindBy
import org.openqa.selenium.support.PageFactory
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import java.util.regex.Pattern


const val ITEM_EVENT_TYPE_ID = "__item4-__xmlview2--CICO_TIME_EVENT_LIST-%i-titleText-inner"
const val ITEM_TIME_ID       = "__item4-__xmlview2--CICO_TIME_EVENT_LIST-%i-ObjectNumber"
const val ITEM_DEL_BUTTON_ID = "__item4-__xmlview2--CICO_TIME_EVENT_LIST-%i-imgDel-inner"

class WorkingTimeCorrectionsPage(private val driver: WebDriver) {

    @FindBy(id = "__xmlview2--CICO_DATE_PICKER-inner")
    private val dateInputField: WebElement? = null

    @FindBy(id = "__xmlview2--CICO_EVENT_TYPES-inner")
    private val eventTypeInput: WebElement? = null

    @FindBy(id = "__xmlview2--CICO_TIME-Picker-inner")
    private val timeInput: WebElement? = null

    @FindBy(id = "__xmlview2--CICO_SAVE_BTN")
    private val saveButton: WebElement? = null

    @FindBy(id = "__xmlview2--CICO_PREVIOUS_EVENTS_FORM_CONTAINER--title")
    private val currentEventsTitleDiv: WebElement? = null

    @FindBy(id = "__button55")
    private val confirmDeleteButton: WebElement? = null

    @FindBy(id = "__button51")
    private val confirmSaveButton: WebElement? = null

    init {
        PageFactory.initElements(driver, this)
    }

    fun getCurrentEventCount(): Int {
        return currentEventsTitleDiv?.text?.split("(")?.get(1)?.substringBefore(")")?.toInt()?:0
    }

    fun selectDate(date: LocalDate) {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.GERMANY)
            .withZone(ZoneId.systemDefault())

        val dateString = formatter.format(date)

        dateInputField?.clear()
        dateInputField?.sendKeys(dateString)
        dateInputField?.sendKeys(Keys.RETURN)
    }

    fun clearAllEvents(maxListIdx: Int) {
        for (idx in 0 until maxListIdx) {
            driver.findElement(By.id(String.format(ITEM_DEL_BUTTON_ID, idx))).submit()
            confirmDeleteButton?.submit()
        }
    }

    fun createClockInAndCLockOut(workDaySummary: WorkDaySummary?) {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.GERMANY)
            .withZone(ZoneId.systemDefault())

        eventTypeInput?.clear()
        eventTypeInput?.sendKeys("Homeoffice clockin")
        timeInput?.clear()
        timeInput?.sendKeys(formatter.format(workDaySummary?.clockIn?.toLocalTime()))
        saveButton?.submit()
        confirmSaveButton?.submit()

        eventTypeInput?.clear()
        eventTypeInput?.sendKeys("Homeoffice clockout")
        timeInput?.clear()
        timeInput?.sendKeys(formatter.format(workDaySummary?.clockOut?.toLocalTime()))
        saveButton?.submit()
        confirmSaveButton?.submit()
    }

    fun getClockInTime(): LocalTime? {
        val timeString = driver.findElement(By.id(String.format(ITEM_TIME_ID, 1))).text
        return LocalTime.parse(timeString)
    }

    fun getClockOutTime(): Any? {
        val timeString = driver.findElement(By.id(String.format(ITEM_TIME_ID, 0))).text
        return LocalTime.parse(timeString)
    }
}