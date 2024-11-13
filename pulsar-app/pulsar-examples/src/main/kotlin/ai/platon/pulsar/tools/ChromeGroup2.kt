package ai.platon.pulsar.tools

import ai.platon.pulsar.common.browser.WebsiteAccount
import ai.platon.pulsar.protocol.browser.DefaultBrowserComponents
import ai.platon.pulsar.skeleton.common.options.LoadOptionDefaults.browser
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import kotlinx.coroutines.runBlocking

class ChromeGroup2 {
    private val components = DefaultBrowserComponents()
    private val driverFactory = components.driverFactory

    fun loginAll() {
        val browserIds = IntRange(0, 100).map { BrowserId.NEXT_SEQUENTIAL }.distinct().shuffled()
        // call login for each browser id in browserIds, all calling should be done in parallel
        runBlocking {
            browserIds.forEach { browserId: BrowserId ->
                browserId.fingerprint.websiteAccounts.values.forEach { account ->
                    login(browserId, account)
                }
            }
        }
    }

    suspend fun login(browserId: BrowserId, account: WebsiteAccount) {
        val browser = driverFactory.launchBrowser(browserId)
        val fingerprint = browser.id.fingerprint

        browser.newDriver().navigateTo("chrome://version")
        browser.newDriver().navigateTo(fingerprint.source!!)

        val driver3 = browser.newDriver()
        driver3.navigateTo(account.loginURL)
        driver3.waitForNavigation()
        driver3.waitForSelector(account.passwordInputSelector)
        driver3.type(account.usernameInputSelector, account.username)
        driver3.type(account.passwordInputSelector, account.password)
    }
}

fun main() {
    ChromeGroup2().loginAll()
    readlnOrNull()
}
