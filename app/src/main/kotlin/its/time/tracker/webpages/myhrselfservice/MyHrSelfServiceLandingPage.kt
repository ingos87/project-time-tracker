package its.time.tracker.webpages.myhrselfservice

import its.time.tracker.webpages.WebElementService


class MyHrSelfServiceLandingPage(private val webElementService: WebElementService) {
    companion object {
        const val CORRECTIONS_TILE_ID = "__tile1"
    }

    fun clickTimeCorrectionsTile() {
        webElementService.clickOnElementWithId(CORRECTIONS_TILE_ID)
    }
}