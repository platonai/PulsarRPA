package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.protocol.browser.driver.BrowserInstanceManager
import ai.platon.pulsar.protocol.browser.driver.WebDriverControl
import ai.platon.pulsar.protocol.browser.driver.WebDriverFactory
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import ai.platon.pulsar.protocol.browser.emulator.context.SinglePrivacyContextManager

class DefaultWebDriverPoolManager(immutableConfig: ImmutableConfig): WebDriverPoolManager(
        WebDriverFactory(WebDriverControl(immutableConfig), BrowserInstanceManager(), immutableConfig),
        immutableConfig
)

class DefaultBrowserEmulator(
        driverPoolManager: WebDriverPoolManager,
        immutableConfig: ImmutableConfig
): BrowserEmulator(
        driverPoolManager,
        BrowserEmulateEventHandler(driverPoolManager, null, immutableConfig),
        immutableConfig
)

class DefaultBrowserEmulatedFetcher(
        immutableConfig: ImmutableConfig,
        driverPoolManager: WebDriverPoolManager = DefaultWebDriverPoolManager(immutableConfig)
): BrowserEmulatedFetcher(
        SinglePrivacyContextManager(driverPoolManager, immutableConfig),
        driverPoolManager,
        DefaultBrowserEmulator(driverPoolManager, immutableConfig),
        immutableConfig
)
