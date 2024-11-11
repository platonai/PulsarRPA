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
package ai.platon.pulsar.protocol.browser.emulator.impl

import ai.platon.pulsar.common.AppContext
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.emulator.AbstractBrowserFetcher
import ai.platon.pulsar.protocol.browser.emulator.BrowserEmulator
import ai.platon.pulsar.protocol.browser.emulator.ManagedBrowserFetcher
import ai.platon.pulsar.protocol.browser.emulator.context.BrowserPrivacyManager
import ai.platon.pulsar.skeleton.crawl.fetch.FetchResult
import ai.platon.pulsar.skeleton.crawl.fetch.FetchTask
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.skeleton.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.skeleton.crawl.protocol.Response
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
open class UnmanagedBrowserFetcher(
    override val browserEmulator: BrowserEmulator,
    override val conf: ImmutableConfig
): AbstractBrowserFetcher() {
    private val logger = LoggerFactory.getLogger(PrivacyManagedBrowserFetcher::class.java)!!
    
    private val closed = AtomicBoolean()
    private val illegalState = AtomicBoolean()
    override val isActive get() = !illegalState.get() && !closed.get() && AppContext.isActive
    
    /**
     * Fetch page content
     * */
    @Throws(Exception::class)
    override suspend fun fetchContentDeferred(page: WebPage): Response {
        if (!isActive) {
            return ForwardingResponse.canceled(page)
        }
        
        if (page.isInternal) {
            logger.warn("Unexpected internal page | {}", page.url)
            return ForwardingResponse.canceled(page)
        }
        
        val driver = page.getVar("WEB_DRIVER") as? WebDriver ?: throw WebDriverException("No web driver found in WebPage")
        
        val task = FetchTask.create(page)
        return fetchDeferred(task, driver).response
    }
    
    @Throws(Exception::class)
    override suspend fun fetchDeferred(url: String, driver: WebDriver): FetchResult {
        return fetchDeferred(FetchTask.create(url, conf.toVolatileConfig()), driver)
    }
    
    override fun reset() {
        TODO("Not implemented")
    }
    
    override fun cancel(page: WebPage) {
        TODO("Not implemented")
    }
    
    override fun cancelAll() {
        TODO("Not implemented")
    }
    
    override fun close() {
        if (closed.compareAndSet(false, true)) {
        }
    }
}

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
open class PrivacyManagedBrowserFetcher(
    override val privacyManager: BrowserPrivacyManager,
    override val browserEmulator: BrowserEmulator,
    override val conf: ImmutableConfig,
    private val closeCascaded: Boolean = false
): AbstractBrowserFetcher(), ManagedBrowserFetcher {
    private val logger = LoggerFactory.getLogger(PrivacyManagedBrowserFetcher::class.java)!!
    
    private val closed = AtomicBoolean()
    private val illegalState = AtomicBoolean()
    override val isActive get() = !illegalState.get() && !closed.get() && AppContext.isActive

    /**
     * Fetch page content
     * */
    @Throws(Exception::class)
    override suspend fun fetchContentDeferred(page: WebPage): Response {
        if (!isActive) {
            return ForwardingResponse.canceled(page)
        }
        
        if (page.isInternal) {
            logger.warn("Unexpected internal page | {}", page.url)
            return ForwardingResponse.canceled(page)
        }
        
        val task = FetchTask.create(page)
        return fetchDeferred(task)
    }
    
    /**
     * Fetch page content.
     * */
    @Throws(Exception::class)
    suspend fun fetchDeferred(task: FetchTask): Response {
        // TODO: it's a temporary solution to specify the web driver to fetch the page
        val driver = task.page.getVar("WEB_DRIVER") as? WebDriver
        if (driver != null) {
            return fetchDeferred(task, driver).response
        }
        
        // @Throws(ProxyException::class, Exception::class)
        return privacyManager.run(task) { _, driver2 -> fetchDeferred(task, driver2) }.response
    }

    @Throws(Exception::class)
    override suspend fun fetchDeferred(url: String, driver: WebDriver): FetchResult {
        return fetchDeferred(FetchTask.create(url, conf.toVolatileConfig()), driver)
    }

    override fun reset() {
        TODO("Not implemented")
    }

    override fun cancel(page: WebPage) {
        TODO("Not implemented")
    }

    override fun cancelAll() {
        TODO("Not implemented")
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            if (closeCascaded) {
                browserEmulator.close()
                privacyManager.close()
            }
        }
    }
}
