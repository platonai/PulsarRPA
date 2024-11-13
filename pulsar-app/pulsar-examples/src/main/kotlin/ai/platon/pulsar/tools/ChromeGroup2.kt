package ai.platon.pulsar.tools

import ai.platon.pulsar.protocol.browser.DefaultBrowserComponents
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import kotlinx.coroutines.runBlocking

fun main() {
    val components = DefaultBrowserComponents()
    val driverFactory = components.driverFactory
    val url = "https://www.taobao.com"

    repeat(6) {
        val browserId = BrowserId.NEXT_SEQUENTIAL
        val fingerprintFile = browserId.contextDir.resolve("fingerprint.json")
        val fingerprint = browserId.fingerprint
        val browser = driverFactory.launchBrowser(BrowserId.NEXT_SEQUENTIAL)

        runBlocking {
            val driver = browser.newDriver()
            driver.navigateTo("chrome://version")

            val driver2 = browser.newDriver()
            driver2.navigateTo(fingerprintFile.toString())

            val driver3 = browser.newDriver()
            driver3.navigateTo(url)
            driver3.waitForNavigation()
            driver3.waitForSelector("body")
            driver3.click("a[href*=login]")
            driver3.waitForNavigation()
            driver3.type("input[name=fm-login-id]", fingerprint.username ?: "")
            driver3.type("input[name=fm-login-password]", fingerprint.password ?: "")
        }
    }

    readlnOrNull()
}
