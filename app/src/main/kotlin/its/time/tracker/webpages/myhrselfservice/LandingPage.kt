package its.time.tracker.webpages.myhrselfservice

import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.FindBy
import org.openqa.selenium.support.PageFactory

class LandingPage(driver: WebDriver) {

    @FindBy(id = "__tile2")
    private val myTimeCorrectionsTile: WebElement? = null

    init {
        PageFactory.initElements(driver, this)
    }

    fun clickTimeCorrectionsTile() {
        myTimeCorrectionsTile?.click()
    }
}