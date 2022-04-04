package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.protocol.browser.driver.BrowserInstanceManager
import ai.platon.pulsar.protocol.browser.driver.WebDriverFactory
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import ai.platon.pulsar.protocol.browser.driver.WebDriverSettings
import ai.platon.pulsar.protocol.browser.emulator.context.BasicPrivacyContextManager

class DefaultWebDriverSettings(conf: ImmutableConfig): WebDriverSettings(conf)

class DefaultBrowserInstanceManager(conf: ImmutableConfig): BrowserInstanceManager(conf)

class DefaultWebDriverFactory(conf: ImmutableConfig)
    : WebDriverFactory(DefaultWebDriverSettings(conf), DefaultBrowserInstanceManager(conf), conf)

class DefaultWebDriverPoolManager(conf: ImmutableConfig)
    : WebDriverPoolManager(DefaultWebDriverFactory(conf), conf, suppressMetrics = true) {

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
        BasicPrivacyContextManager(driverPoolManager, conf),
        driverPoolManager,
        DefaultBrowserEmulator(driverPoolManager, conf),
        conf,
        closeCascaded = true
)
