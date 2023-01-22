package its.time.tracker.webpages.myhrselfservice

import its.time.tracker.config.Constants
import its.time.tracker.config.printDebug
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.FindBy
import org.openqa.selenium.support.PageFactory
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration


class LandingPage(private val driver: WebDriver) {
    companion object {
        const val CORRECTIONS_TILE_ID = "__tile1-content"
        const val CORRECTIONS_TILE_TEXT = "My time corrections"
        // WebElement e = driver.findElement(By.xpath("//*[text()='Get started free']"));
    }

    //@FindBy(xpath = "//*[text()='${CORRECTIONS_TILE_TEXT}']")
    @FindBy(id = CORRECTIONS_TILE_ID)
    private val myTimeCorrectionsTile: WebElement? = null

    init {
        PageFactory.initElements(driver, this)
    }

    fun clickTimeCorrectionsTile() {
        val tile: WebElement = WebDriverWait(driver, Duration.ofSeconds(20))
            .until(ExpectedConditions.elementToBeClickable(By.xpath("//*[text()='${CORRECTIONS_TILE_TEXT}']")))
        tile.click()
        printDebug("clicked on My time corrections tile")
    }
}