package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyManager
import ai.platon.pulsar.protocol.browser.driver.BrowserManager
import ai.platon.pulsar.protocol.browser.driver.WebDriverFactory
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import ai.platon.pulsar.protocol.browser.driver.WebDriverSettings
import ai.platon.pulsar.protocol.browser.emulator.context.BasicPrivacyContextManager

class DefaultWebDriverSettings(conf: ImmutableConfig): WebDriverSettings(conf)

class DefaultBrowserManager(conf: ImmutableConfig): BrowserManager(conf)

class DefaultWebDriverFactory(conf: ImmutableConfig)
    : WebDriverFactory(DefaultWebDriverSettings(conf), DefaultBrowserManager(conf), conf)

class DefaultWebDriverPoolManager(conf: ImmutableConfig)
    : WebDriverPoolManager(DefaultWebDriverFactory(conf), conf, suppressMetrics = true) {

    }

class DefaultBrowserEmulator(
        driverPoolManager: WebDriverPoolManager,
        conf: ImmutableConfig
): BrowserEmulator(
        driverPoolManager,
        BrowserResponseHandler(driverPoolManager, null, conf),
        conf
)

class DefaultBrowserEmulatedFetcher(
        conf: ImmutableConfig,
        driverPoolManager: WebDriverPoolManager = DefaultWebDriverPoolManager(conf)
): BrowserEmulatedFetcher(
        BasicPrivacyContextManager(driverPoolManager, conf),
        driverPoolManager,
        DefaultBrowserEmulator(driverPoolManager, conf),
        conf,
        closeCascaded = true
)

class Defaults(val conf: ImmutableConfig) {
    companion object {
        private var defaultFetcher: DefaultBrowserEmulatedFetcher? = null
    }

    private fun getOrCreateBrowserEmulatedFetcher(): DefaultBrowserEmulatedFetcher {
        synchronized(this) {
            if (defaultFetcher == null) {
                defaultFetcher = DefaultBrowserEmulatedFetcher((conf))
            }
            return defaultFetcher!!
        }
    }

    val browserEmulatedFetcher: BrowserEmulatedFetcher
        get() = getOrCreateBrowserEmulatedFetcher()

    val browserEmulator: BrowserEmulator
        get() = browserEmulatedFetcher.browserEmulator

    val privacyManager: PrivacyManager
        get() = browserEmulatedFetcher.privacyManager

    val driverPoolManager: WebDriverPoolManager
        get() = browserEmulatedFetcher.driverPoolManager

    val driverFactory: WebDriverFactory
        get() = driverPoolManager.driverFactory

    val browserManager: BrowserManager
        get() = driverFactory.browserManager
}
