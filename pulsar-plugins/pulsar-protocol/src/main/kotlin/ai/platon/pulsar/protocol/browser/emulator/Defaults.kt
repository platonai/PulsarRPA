package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.protocol.browser.driver.BrowserInstanceManager
import ai.platon.pulsar.protocol.browser.driver.WebDriverControl
import ai.platon.pulsar.protocol.browser.driver.WebDriverFactory
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import ai.platon.pulsar.protocol.browser.emulator.context.SinglePrivacyContextManager

class DefaultWebDriverControl(conf: ImmutableConfig): WebDriverControl(conf)

class DefaultWebDriverFactory(conf: ImmutableConfig)
    : WebDriverFactory(DefaultWebDriverControl(conf), BrowserInstanceManager(), conf)

class DefaultWebDriverPoolManager(conf: ImmutableConfig)
    : WebDriverPoolManager(DefaultWebDriverFactory(conf), conf) {
    init {
        suppressMetrics = true
    }
}

class DefaultBrowserEmulator(
        driverPoolManager: WebDriverPoolManager,
        conf: ImmutableConfig
): BrowserEmulator(
        driverPoolManager,
        EventHandler(driverPoolManager, null, conf),
        conf
)

class DefaultBrowserEmulatedFetcher(
        conf: ImmutableConfig,
        driverPoolManager: WebDriverPoolManager = DefaultWebDriverPoolManager(conf)
): BrowserEmulatedFetcher(
        SinglePrivacyContextManager(driverPoolManager, conf),
        driverPoolManager,
        DefaultBrowserEmulator(driverPoolManager, conf),
        conf
)
