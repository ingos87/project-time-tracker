package project.time.tracker.webpages.myhrselfservice

import project.time.tracker.webpages.WebElementService


class MyHrSelfServiceLandingPage(private val webElementService: WebElementService) {
    companion object {
        const val CORRECTIONS_TILE_ID = "__tile1"
        const val MODAL_DIALOG_CANCEL_BUTTON_ID = "cancelBtn"
    }

    fun clickTimeCorrectionsTile() {
        webElementService.clickOnElementWithId(CORRECTIONS_TILE_ID)
    }

    fun ensureAllModalsAreClosed() {
        repeat(8) {
            val button = webElementService.findElementById(MODAL_DIALOG_CANCEL_BUTTON_ID)
            if (button != null) {
                webElementService.clickOnElementWithId(MODAL_DIALOG_CANCEL_BUTTON_ID)
                return
            }
            Thread.sleep(500)
        }
    }
}