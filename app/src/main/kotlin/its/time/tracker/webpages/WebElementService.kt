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

    private fun clickOnElementBy(by: By, retryCount: Int = DEFAULT_RETRY_COUNT) {
        try {
            val elem: WebElement = WebDriverWait(webDriver, Duration.ofSeconds(DEFAULT_WAIT_SECONDS))
                .until(ExpectedConditions.elementToBeClickable(by))
            elem.click()
        } catch (e: Exception) {
            when(e) {
                is StaleElementReferenceException,
                is ElementNotInteractableException -> {
                    if (retryCount > 0) {
                        clickOnElementBy(by, retryCount-1)
                        return
                    }
                }
                else -> throw e
            }
        }
    }

    fun getElementTextualContent(elementId: String, retryCount: Int = DEFAULT_RETRY_COUNT): String {
        try {
            val div: WebElement = WebDriverWait(webDriver, Duration.ofSeconds(DEFAULT_WAIT_SECONDS))
                .until(ExpectedConditions.elementToBeClickable(By.id(elementId)))
            printDebug("found content of element id($elementId): '${div.text}'")
            return div.text
        } catch (e: Exception) {
            when(e) {
                is StaleElementReferenceException,
                is ElementNotInteractableException -> {
                    if (retryCount > 0) {
                        return getElementTextualContent(elementId, retryCount-1)
                    }
                }
                else -> throw e
            }
        }
        return ""
    }

    fun setTextualContent(elementId: String, string: String) {
        clearField(elementId)
        string.toCharArray().forEach {
            Thread.sleep(20)
            sendCharacter(elementId, "" + it)
        }
        printDebug("successfully inserted text into input id($elementId): $string")
    }

    fun sendCharacter(elementId: String, character: CharSequence, retryCount: Int = DEFAULT_RETRY_COUNT) {
        try {
            val inputField: WebElement = WebDriverWait(webDriver, Duration.ofSeconds(DEFAULT_WAIT_SECONDS))
                .until(ExpectedConditions.elementToBeClickable(By.id(elementId)))
            inputField.sendKeys(character)
            printDebug("sent character to input id($elementId): $character")
        } catch (e: Exception) {
            when(e) {
                is StaleElementReferenceException,
                is ElementNotInteractableException -> {
                    if (retryCount > 0) {
                        sendCharacter(elementId, character, retryCount-1)
                        return
                    }
                }
                else -> throw e
            }
        }
    }

    private fun clearField(elementId: String, retryCount: Int = DEFAULT_RETRY_COUNT) {
        try {
            val inputField: WebElement = WebDriverWait(webDriver, Duration.ofSeconds(DEFAULT_WAIT_SECONDS))
                .until(ExpectedConditions.elementToBeClickable(By.id(elementId)))
            // TODO needs rework ... takes too long
            inputField.clear()
            Thread.sleep(30)
            repeat(30) {
                inputField.sendKeys(Keys.DELETE)
                inputField.sendKeys(Keys.BACK_SPACE)
                Thread.sleep(50)
            }
            Thread.sleep(30)
            printDebug("cleared text of element id($elementId)")
        } catch (e: Exception) {
            when(e) {
                is StaleElementReferenceException,
                is ElementNotInteractableException -> {
                    if (retryCount > 0) {
                        clearField(elementId, retryCount-1)
                        return
                    }
                }
                else -> throw e
            }
        }
    }
}