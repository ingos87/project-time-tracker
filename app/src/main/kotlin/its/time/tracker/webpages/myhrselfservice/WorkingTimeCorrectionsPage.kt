package its.time.tracker.webpages.myhrselfservice

import its.time.tracker.config.printDebug
import its.time.tracker.domain.EventType
import its.time.tracker.domain.WorkDaySummary
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
import java.util.*


const val ITEM_EVENT_TYPE_ID = "__item4-__xmlview2--CICO_TIME_EVENT_LIST-%i-titleText-inner"
const val ITEM_TIME_ID       = "__item4-__xmlview2--CICO_TIME_EVENT_LIST-%i-ObjectNumber"
const val ITEM_DEL_BUTTON_ID = "__item4-__xmlview2--CICO_TIME_EVENT_LIST-%i-imgDel-inner"

class WorkingTimeCorrectionsPage(private val driver: WebDriver) {

    companion object {
        private const val DATE_INPUT_FIELD_ID = "__xmlview2--CICO_DATE_PICKER-inner"
        private const val CURRENT_EVENTS_TITLE_DIV_ID = "__xmlview2--CICO_PREVIOUS_EVENTS_FORM_CONTAINER--title"
        private const val CONFIRM_BUTTON_ID = "__button51"
        private const val EVENT_TYPE_INPUT_ID = "__xmlview2--CICO_EVENT_TYPES-inner"
    }

    @FindBy(id = DATE_INPUT_FIELD_ID)
    private val dateInputField: WebElement? = null

    @FindBy(id = EVENT_TYPE_INPUT_ID)
    private val eventTypeInput: WebElement? = null

    @FindBy(id = "__xmlview2--CICO_TIME-Picker-inner")
    private val timeInput: WebElement? = null

    @FindBy(id = "__xmlview2--CICO_SAVE_BTN")
    private val saveButton: WebElement? = null

    @FindBy(id = CURRENT_EVENTS_TITLE_DIV_ID)
    private val currentEventsTitleDiv: WebElement? = null

    @FindBy(id = "__button55")
    private val confirmDeleteButton: WebElement? = null

    @FindBy(id = CONFIRM_BUTTON_ID)
    private val confirmSaveButton: WebElement? = null

    init {
        PageFactory.initElements(driver, this)
    }

    fun getCurrentEventCount(): Int {
        val titleDiv: WebElement = WebDriverWait(driver, Duration.ofSeconds(20))
            .until(ExpectedConditions.elementToBeClickable(By.id(CURRENT_EVENTS_TITLE_DIV_ID)))
        if (!titleDiv.text.contains("(")) {
            return 0
        }
        return titleDiv.text.split("(")[1].substringBefore(")").toInt()
    }

    fun selectDate(date: LocalDate) {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.GERMANY)
            .withZone(ZoneId.systemDefault())

        val dateString = formatter.format(date)

        val inputField: WebElement = WebDriverWait(driver, Duration.ofSeconds(20))
            .until(ExpectedConditions.elementToBeClickable(By.id(DATE_INPUT_FIELD_ID)))
        printDebug("current date input value: " + inputField.getAttribute("value"))
        clearField(inputField)
        inputField.sendKeys(dateString)
        inputField.sendKeys(Keys.ENTER)

        WebDriverWait(driver, Duration.ofSeconds(20))
            .until(ExpectedConditions.visibilityOfElementLocated(By.id(EVENT_TYPE_INPUT_ID)))

        printDebug("successfully inserted date: $dateString")
    }

    private fun clearField(inputField: WebElement) {
        inputField.clear()
        repeat(20) {
            inputField.sendKeys(Keys.DELETE)
            inputField.sendKeys(Keys.BACK_SPACE)
            Thread.sleep(10)
        }
    }

    fun clearAllEvents(maxListIdx: Int) {
        for (idx in 0 until maxListIdx) {
            driver.findElement(By.id(String.format(ITEM_DEL_BUTTON_ID, idx))).submit()
            confirmDeleteButton?.submit()
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
        val typeField: WebElement = WebDriverWait(driver, Duration.ofSeconds(20))
            .until(ExpectedConditions.elementToBeClickable(By.id(EVENT_TYPE_INPUT_ID)))
        clearField(typeField)
        printDebug("cleared clock type")
        typeField.sendKeys(eventTypeText)
        printDebug("inserted clock type: $eventTypeText")

        //timeInput?.clear()
        timeInput?.sendKeys(timeString.replace(":", ""))
        printDebug("inserted time: $timeString")

        saveButton?.click()
        printDebug("triggered event save")

        val confirmButton: WebElement = WebDriverWait(driver, Duration.ofSeconds(20))
            .until(ExpectedConditions.elementToBeClickable(By.id(CONFIRM_BUTTON_ID)))
        confirmButton.click()
        printDebug("confirmed event save")
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