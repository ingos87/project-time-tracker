package its.time.tracker.webpages

import its.time.tracker.config.Constants
import its.time.tracker.config.printDebug
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration


class WebElementService {

    private val webDriver: WebDriver

    companion object {
        private const val DEFAULT_WAIT_SECONDS = 15L
        private const val DEFAULT_RETRY_COUNT = 10
    }

    init {
        val options = ChromeOptions()
        options.addArguments("user-data-dir=${Constants.CHROME_PROFILE_PATH}")
        webDriver = ChromeDriver(options)
        webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(DEFAULT_WAIT_SECONDS))
        printDebug(webDriver.toString())
    }

    fun navigateToUrl(url: String) {
        webDriver.get(url)
        printDebug("navigated to $url")
    }

    fun clickOnElementWithId(id: String) {
        clickOnElementBy(By.id(id))
        printDebug("clicked on element with id '$id'")
    }

    fun clickOnElementWithText(text: String) {
        clickOnElementBy(By.xpath("//*[text()='$text']"))
        printDebug("clicked on element with text '$text'")
    }

    fun clickOnAllElementWithTitle(title: String) {
        val elements: List<WebElement> = webDriver.findElements(By.xpath("//*[@title='$title']"))
        elements.forEach{
            val id = it.getAttribute("id")
            clickOnElementWithId(id)
            printDebug("clicked on element with id '$id'")
        }
    }

    private fun clickOnElementBy(by: By): String {
        val clickFun: (By, String) -> String = { funBy, _ ->
            val elem = waitForElementToBeClickable(funBy)
            elem.click();""
        }

        return retryActionUntilSuccess(clickFun, by, "")
    }

    fun getElementTextualContent(elementId: String): String {
        val getTextFun: (By, String) -> String = { funBy, _ ->
            val div = waitForElementToBeClickable(funBy)
            printDebug("found content of element id($elementId): '${div.text}'")
            div.text
        }

        return retryActionUntilSuccess(getTextFun, By.id(elementId), "")
    }

    fun setTextualContent(elementId: String, string: String) {
        clearField(elementId, string.length)
        string.toCharArray().forEach {
            Thread.sleep(20)
            sendCharacter(elementId, "" + it)
        }
        printDebug("successfully inserted text into input id($elementId): $string")
    }

    fun waitForElementToBeClickable(by: By): WebElement {
        return WebDriverWait(webDriver, Duration.ofSeconds(DEFAULT_WAIT_SECONDS))
            .until(ExpectedConditions.elementToBeClickable(by))
    }

    private fun sendCharacter(elementId: String, singleCharacter: String): String {
        val sendCharFun: (By, String) -> String = { funBy, funChar ->
            val inputField = waitForElementToBeClickable(funBy)
            inputField.sendKeys(funChar)
            printDebug("sent character to input id($funBy): $funChar");""
        }

        return retryActionUntilSuccess(sendCharFun, By.id(elementId), singleCharacter)
    }

    private fun clearField(elementId: String, expectedCharsToByCleared: Int): String {
        val clearFun: (By, String) -> String = { funBy, charCount ->
            val inputField = waitForElementToBeClickable(funBy)
            inputField.clear()
            repeat(charCount.toInt() * 2) {
                inputField.sendKeys(Keys.DELETE)
                inputField.sendKeys(Keys.BACK_SPACE)
                Thread.sleep(20)
            }
            Thread.sleep(15)
            printDebug("cleared text of element id($elementId)");""
        }

        return retryActionUntilSuccess(clearFun, By.id(elementId), expectedCharsToByCleared.toString())
    }

    private fun retryActionUntilSuccess(action: (By, String) -> String,
                                        by: By,
                                        param: String,
                                        retryCount: Int = DEFAULT_RETRY_COUNT): String {
        try {
            return action(by, param)
        } catch (e: Exception) {
            when(e) {
                is StaleElementReferenceException,
                is ElementNotInteractableException -> {
                    if (retryCount > 0) {
                        return retryActionUntilSuccess(action, by, param, retryCount-1)
                    }
                }
                else -> throw e
            }
        }
        return ""
    }
}