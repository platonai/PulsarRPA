package ai.platon.pulsar.tools

import ai.platon.pulsar.protocol.browser.DefaultBrowserComponents
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import kotlinx.coroutines.runBlocking

fun main() {
    val components = DefaultBrowserComponents()
    val driverFactory = components.driverFactory
    val url = "https://www.taobao.com"

    val browserIds = IntRange(0, 5).map { BrowserId.NEXT_SEQUENTIAL }.shuffled()
    browserIds.forEach { browserId ->
        val browser = driverFactory.launchBrowser(browserId)
        val fingerprint = browser.id.fingerprint

        runBlocking {
            browser.newDriver().navigateTo("chrome://version")
            browser.newDriver().navigateTo(fingerprint.source!!)

            val driver3 = browser.newDriver()
            driver3.navigateTo(url)
            driver3.waitForNavigation()
            driver3.waitForSelector("a[href*=login]")
            driver3.click("a[href*=login]")
            driver3.waitForNavigation()
            driver3.waitForSelector("input[name=fm-login-password]")
            driver3.type("input[name=fm-login-id]", fingerprint.username ?: "unspecified")
            driver3.type("input[name=fm-login-password]", fingerprint.password ?: "unspecified")
        }
    }

    readlnOrNull()
}
