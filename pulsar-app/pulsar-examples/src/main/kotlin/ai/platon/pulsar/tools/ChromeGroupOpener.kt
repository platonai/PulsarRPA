package ai.platon.pulsar.tools

import ai.platon.pulsar.common.browser.WebsiteAccount
import ai.platon.pulsar.protocol.browser.DefaultBrowserComponents
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ChromeGroupOpener {
    private val components = DefaultBrowserComponents()
    private val driverFactory = components.driverFactory

    fun open() {
        val browserIds = IntRange(0, 100).map { BrowserId.NEXT_SEQUENTIAL }.distinct().shuffled()
        // call login for each browser id in browserIds, all calling should be done in parallel
        runBlocking {
            browserIds.forEach { browserId: BrowserId ->
                browserId.fingerprint.websiteAccounts.values.forEach { account ->
                    launch {
                        kotlin.runCatching { login(browserId, account) }.onFailure { it.printStackTrace() }
                    }
                }
            }
        }
    }

    private suspend fun login(browserId: BrowserId, account: WebsiteAccount) {
        val browser = driverFactory.launchBrowser(browserId)
        val fingerprint = browser.id.fingerprint

        browser.newDriver().navigateTo("chrome://version")
        browser.newDriver().navigateTo(fingerprint.source!!)

        browser.newDriver().apply {
            navigateTo(account.homeURL)
            waitForSelector(account.loginLinkSelector)
            click(account.loginLinkSelector)
            waitForNavigation()
            waitForSelector(account.passwordInputSelector)
            type(account.usernameInputSelector, account.username)
            type(account.passwordInputSelector, account.password)
        }
    }
}

fun main() {
    ChromeGroupOpener().open()
    readlnOrNull()
}
