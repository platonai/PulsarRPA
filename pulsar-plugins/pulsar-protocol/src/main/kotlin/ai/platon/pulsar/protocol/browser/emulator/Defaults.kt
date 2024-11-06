/**
 * Copyright (c) Vincent Zhang, ivincent.zhang@gmail.com, Platon.AI.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.PrivacyManager
import ai.platon.pulsar.protocol.browser.driver.BrowserManager
import ai.platon.pulsar.protocol.browser.driver.WebDriverFactory
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import ai.platon.pulsar.protocol.browser.driver.WebDriverSettings
import ai.platon.pulsar.protocol.browser.emulator.context.BasicPrivacyContextManager
import ai.platon.pulsar.protocol.browser.emulator.impl.BrowserEmulatedFetcherImpl
import ai.platon.pulsar.protocol.browser.emulator.impl.InteractiveBrowserEmulator
import ai.platon.pulsar.protocol.browser.emulator.impl.BrowserResponseHandlerImpl

class DefaultWebDriverSettings(conf: ImmutableConfig): WebDriverSettings(conf)

class DefaultBrowserManager(conf: ImmutableConfig): BrowserManager(conf)

class DefaultWebDriverFactory(conf: ImmutableConfig)
    : WebDriverFactory(DefaultWebDriverSettings(conf), DefaultBrowserManager(conf), conf)

class DefaultWebDriverPoolManager(conf: ImmutableConfig) :
    WebDriverPoolManager(
        DefaultBrowserManager(conf),
        DefaultWebDriverFactory(conf),
        conf, suppressMetrics = true
    )

class DefaultBrowserEmulator(
        driverPoolManager: WebDriverPoolManager,
        conf: ImmutableConfig
): InteractiveBrowserEmulator(
        driverPoolManager,
        BrowserResponseHandlerImpl(conf),
        conf
)

class DefaultBrowserEmulatedFetcher(
        conf: ImmutableConfig,
        driverPoolManager: WebDriverPoolManager = DefaultWebDriverPoolManager(conf)
): BrowserEmulatedFetcherImpl(
        BasicPrivacyContextManager(driverPoolManager, conf),
        driverPoolManager,
        DefaultBrowserEmulator(driverPoolManager, conf),
        conf,
        closeCascaded = true
)

class Defaults(val conf: ImmutableConfig) {
    companion object {
        private var fetcher: BrowserEmulatedFetcher? = null
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

    private fun getOrCreateBrowserEmulatedFetcher(): BrowserEmulatedFetcher {
        synchronized(this) {
            if (fetcher == null) {
                fetcher = DefaultBrowserEmulatedFetcher((conf))
            }
            return fetcher!!
        }
    }
}
