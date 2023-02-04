package its.time.tracker.webpages

import its.time.tracker.config.Constants
import its.time.tracker.config.printDebug
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration
import java.util.regex.Pattern


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
        webDriver.manage().timeouts().implicitlyWait(Duration.ofMillis(500L))
        printDebug(webDriver.toString())
    }

    fun navigateToUrl(url: String) {
        webDriver.get(url)
        printDebug("navigated to $url")
    }

    fun clickOnElementWithId(id: String, waitSeconds: Long = DEFAULT_WAIT_SECONDS) {
        clickOnElementBy(By.id(id), waitSeconds)
        printDebug("clicked on element with id '$id'")
    }

    fun clickOnElementWithText(text: String, waitSeconds: Long = DEFAULT_WAIT_SECONDS) {
        clickOnElementBy(By.xpath("//*[text()='$text']"), waitSeconds)
        printDebug("clicked on element with text '$text'")
    }

    fun clickOnAllElementWithTitle(title: String) {
        val elements: List<WebElement> = webDriver.findElements(By.xpath("//*[@title='$title']"))
        printDebug("found ${elements.size} expand buttons")
        elements.forEach{
            it.click()
            printDebug("clicked on expand button $it")
        }
    }

    private fun clickOnElementBy(by: By, waitSeconds: Long = DEFAULT_WAIT_SECONDS): String {
        val clickFun: (By, String) -> String = { funBy, _ ->
            val elem = waitForElementToBeClickable(funBy, waitSeconds)
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

    fun findElementByRegexId(regex: String): WebElement? {
        val pattern = Pattern.compile(regex)

        val elements = webDriver.findElements(By.xpath("//*[@id]"))
        printDebug("found ${elements.size} elements by id")
        printDebug("ids: " + elements.joinToString { it.getAttribute("id") })
        for (element in elements) {
            val id = element.getAttribute("id")
            val matcher = pattern.matcher(id)
            if (matcher.matches()) {
                return element
            }
        }
        return null
    }

    fun findElementByIdComponents(prefix: String, suffix: String, startingIdx: Int): WebElement? {
        // TODO fix time issues if first found index is too large
        (startingIdx..200).forEach {
            val maybeElementId = "$prefix$it$suffix"
            val webElem = findElementById(maybeElementId)
            if (webElem != null) {
                printDebug("found element $maybeElementId")
                return webElem
            }
        }
        return null
    }

    fun findElementById(id: String): WebElement? {
        return try {
            val by = By.id(id)
            WebDriverWait(webDriver, Duration.ofMillis(100L)).until(ExpectedConditions.elementToBeClickable(by))
            webDriver.findElement(by)
        } catch (e: NoSuchElementException) {
            null
        } catch (e: TimeoutException) {
            null
        }
    }

    fun waitForElementToBeClickable(by: By, waitSeconds: Long = DEFAULT_WAIT_SECONDS): WebElement {
        return WebDriverWait(webDriver, Duration.ofSeconds(waitSeconds))
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