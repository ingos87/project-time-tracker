package its.time.tracker.webpages.myhrselfservice

import its.time.tracker.webpages.WebElementService


class MyHrSelfServiceLandingPage(private val webElementService: WebElementService) {
    companion object {
        const val CORRECTIONS_TILE_TEXT = "My time corrections"
    }

    fun clickTimeCorrectionsTile() {
        webElementService.clickOnElementWithText(CORRECTIONS_TILE_TEXT)
    }
}