package its.time.tracker.webpages.etime

import its.time.tracker.webpages.WebElementService


class ETimeLandingPage(private val webElementService: WebElementService) {

    companion object {
        const val E_TIME_TILE_ID = "__tile4"
    }

    fun clickETimeTile(waitSeconds: Long) {
        webElementService.clickOnElementWithId(E_TIME_TILE_ID, waitSeconds)
    }
}