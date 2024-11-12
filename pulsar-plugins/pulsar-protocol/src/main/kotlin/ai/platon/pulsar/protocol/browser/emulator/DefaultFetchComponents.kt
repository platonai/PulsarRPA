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
import ai.platon.pulsar.protocol.browser.driver.BrowserManager
import ai.platon.pulsar.protocol.browser.driver.WebDriverFactory
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager
import ai.platon.pulsar.protocol.browser.driver.WebDriverSettings
import ai.platon.pulsar.protocol.browser.emulator.context.BasicPrivacyContextManager
import ai.platon.pulsar.protocol.browser.emulator.context.BrowserPrivacyManager
import ai.platon.pulsar.protocol.browser.emulator.impl.BrowserResponseHandlerImpl
import ai.platon.pulsar.protocol.browser.emulator.impl.InteractiveBrowserEmulator
import ai.platon.pulsar.protocol.browser.emulator.impl.PrivacyManagedBrowserFetcher
import ai.platon.pulsar.skeleton.crawl.fetch.WebDriverFetcher

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

class DefaultPrivacyManagedBrowserFetcher(
    privacyManager: BrowserPrivacyManager,
    browserEmulator: BrowserEmulator,
    conf: ImmutableConfig,
    closeCascaded: Boolean = true
): PrivacyManagedBrowserFetcher(
    privacyManager,
    browserEmulator,
    conf,
    closeCascaded
) {
    constructor(conf: ImmutableConfig, driverPoolManager: WebDriverPoolManager = DefaultWebDriverPoolManager(conf)): this(
        BasicPrivacyContextManager(driverPoolManager, conf),
        DefaultBrowserEmulator(driverPoolManager, conf),
        conf,
        closeCascaded = true
    )
    
    constructor(privacyManager: BrowserPrivacyManager, driverPoolManager: WebDriverPoolManager) : this(
        privacyManager,
        DefaultBrowserEmulator(driverPoolManager, privacyManager.conf),
        privacyManager.conf,
        closeCascaded = true
    )
}

class DefaultFetchComponents(val conf: ImmutableConfig = ImmutableConfig()) {
    companion object {
        // TODO: should create one fetcher for each conf object, ObjectCache can be used.
        private var fetcher: IncognitoBrowserFetcher? = null
    }

    val incognitoBrowserFetcher: IncognitoBrowserFetcher
        get() = getOrCreateBrowserEmulatedFetcher()

    val browserEmulator: BrowserEmulator
        get() = incognitoBrowserFetcher.browserEmulator
    
    val webdriverFetcher: WebDriverFetcher
        get() = incognitoBrowserFetcher.webdriverFetcher

    val privacyManager: BrowserPrivacyManager
        get() = incognitoBrowserFetcher.privacyManager

    val driverPoolManager: WebDriverPoolManager
        get() = privacyManager.driverPoolManager

    val driverFactory: WebDriverFactory
        get() = driverPoolManager.driverFactory

    val browserManager: BrowserManager
        get() = driverFactory.browserManager

    private fun getOrCreateBrowserEmulatedFetcher(): IncognitoBrowserFetcher {
        synchronized(this) {
            if (fetcher == null) {
                fetcher = DefaultPrivacyManagedBrowserFetcher(conf)
            }
            return fetcher!!
        }
    }
}
