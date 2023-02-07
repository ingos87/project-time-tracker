package its.time.tracker.webpages.etime

import its.time.tracker.config.Constants
import its.time.tracker.config.printDebug
import its.time.tracker.exception.AbortException
import its.time.tracker.util.DateTimeUtil
import its.time.tracker.webpages.WebElementService
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class ETimeAssessmentPage(private val webElementService: WebElementService) {

    companion object {
        fun getLocalizedSaveButtonText(): String {
            return when(Constants.E_TIME_LANGUAGE) {
                "DE" -> "..." // TODO
                else -> "Save"
            }
        }
        fun getLocalizedSignButtonText(): String {
            return when(Constants.E_TIME_LANGUAGE) {
                "DE" -> "..." // TODO
                else -> "Sign"
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
        webElementService.waitForElementToBeClickable(By.xpath("//*[@title='Collapse']"))
        printDebug("ETimeAssessmentPage finished loading")
    }

    fun addAllTasksAndStandardActivities() {
        Thread.sleep(4000)
        webElementService.clickOnElementWithId("application-YUI_193_ETIME-change-component---idAppControl--myFavButtonID")
        Thread.sleep(500)
    }

    fun clickAllExpandIcons() {
        webElementService.clickOnAllElementWithTitle("Expand")
        Thread.sleep(1000)
        doThingyToEnsurePageLoadingFinished()
        printDebug("expanded all months")
    }

    fun clickSaveButton() {
        try {
            webElementService.clickOnElementWithText(getLocalizedSaveButtonText(), 1L)
            Thread.sleep(5000)
        } catch (e: org.openqa.selenium.TimeoutException) {
            // ignore because page seems to be non-editable
        }
    }

    fun clickSignButton() {
        try {
            webElementService.clickOnElementWithText(getLocalizedSignButtonText())
            Thread.sleep(5000)
        } catch (e: org.openqa.selenium.TimeoutException) {
            // ignore because page seems to be non-editable
        }
    }

    // __link6-Table0-0 -> Table0-0 input55, 56, 57, 58, 59, 60, 61
    // __link7-Table1-0 -> Table1-0 input64, 65, 66, 67, 68, 69, 70
    // __link8-Table2-0 -> Table2-0 input73 ... 79
    // __link9-Table3-0 -> Table3-0 input82 ... 88 (same within section)
    // __link9-Table3-1 -> Table3-1 input82 ... 88 (same within section)
    // __link9-Table3-2 -> Table3-2 input82 ... 88 (same within section)
    // __link10-Table4-0 -> Table4-0 input91 ... 97 (same within section)
    fun getRelevantWebElementIds(): List<CostAssessmentPositionIds> {
        // e.g. __link0-Table0-0
        val firstBookingPositionTitle = webElementService.findElementByIdComponents("__link", "-Table0-0", 0)
            ?: throw AbortException("was unable to find any cost assessment keys on page")
        val firstTableTitleRegexString = "__link(\\d+)-Table0-0"
        val firstTableIdx = getWildcardPartFromElementId(firstBookingPositionTitle, Regex(firstTableTitleRegexString)).toInt()

        // e.g. __input1-Table0-0-inner
        val firstBookingPositionInput = webElementService.findFirstElementInIdSection("__input", "-Table0-0-inner")
            ?: throw AbortException("was unable to find any cost assessment inputs on page")

        val firstBookingPositionInputRegexString = "__input(\\d+)-Table0-0-inner"
        var firstInputId = getWildcardPartFromElementId(firstBookingPositionInput, Regex(firstBookingPositionInputRegexString)).toInt() + 1

        val allTableIndices = findAllTableIndices(firstTableIdx, 0, 0)

        val resultList = mutableListOf<CostAssessmentPositionIds>()
        allTableIndices.forEachIndexed { index, tableLineTitleId ->
            val tableId = tableLineTitleId.substringAfter('e')

            if (tableLineTitleId.last() == '0' && index != 0) {
                firstInputId += 9
            }

            resultList.add(
                CostAssessmentPositionIds(
                    webElementService.getElementTextualContent(tableLineTitleId),
                    mapOf(
                        DayOfWeek.MONDAY    to "__input${firstInputId}-Table$tableId-inner",
                        DayOfWeek.TUESDAY   to "__input${firstInputId+1}-Table$tableId-inner",
                        DayOfWeek.WEDNESDAY to "__input${firstInputId+2}-Table$tableId-inner",
                        DayOfWeek.THURSDAY  to "__input${firstInputId+3}-Table$tableId-inner",
                        DayOfWeek.FRIDAY    to "__input${firstInputId+4}-Table$tableId-inner",
                        DayOfWeek.SATURDAY  to "__input${firstInputId+5}-Table$tableId-inner",
                        DayOfWeek.SUNDAY    to "__input${firstInputId+6}-Table$tableId-inner",
                    )
                )
            )
        }

        return resultList.toList()
    }

    private fun findAllTableIndices(linkIdx: Int, primaryTableIdx: Int, secondaryTableIdx: Int): List<String> {
        if (linkIdx >= 300 || primaryTableIdx >= 100) {
            return emptyList()
        }

        val currentId = "__link$linkIdx-Table$primaryTableIdx-$secondaryTableIdx"
        val firstBookingPositionTitle = webElementService.findElementById(currentId)
        if (firstBookingPositionTitle != null) {
            return listOf(currentId) + findAllTableIndices(linkIdx, primaryTableIdx, secondaryTableIdx+1)
        }
        printDebug("element with id $currentId not found. Try next section ...")

        if (secondaryTableIdx > 0) {
            return findAllTableIndices(linkIdx+1, primaryTableIdx+1, 0)
        }

        return emptyList()
    }

    private fun getWildcardPartFromElementId(element: WebElement, pattern: Regex): String {
        val id: String = element.getAttribute("id")
        val matchResult = pattern.find(id)
        if (matchResult != null) {
            return matchResult.groupValues[1]
        }
        else {
            throw AbortException("Error occurred during web element id wild card extraction: $id")
        }
    }

    fun insertHours(elementId: String, hoursString: String) {
        webElementService.setTextualContent(elementId, hoursString)
    }
}

data class CostAssessmentPositionIds(
    val bookingPositionKey: String,
    val inputIdMap: Map<DayOfWeek, String>,
)