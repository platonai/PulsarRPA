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

import ai.platon.pulsar.common.brief
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.stringify
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.skeleton.common.persist.ext.browseEventHandlers
import ai.platon.pulsar.skeleton.crawl.fetch.FetchResult
import ai.platon.pulsar.skeleton.crawl.fetch.FetchTask
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriverCancellationException
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriverException
import ai.platon.pulsar.skeleton.crawl.protocol.Response
import kotlinx.coroutines.runBlocking

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
abstract class AbstractBrowserFetcher: BrowserEmulatedFetcher {
    
    enum class EventType {
        willFetch,
        fetched
    }
    
    private val logger = getLogger(this::class)
    
    abstract val isActive: Boolean
    
    @Throws(Exception::class)
    override fun fetch(url: String) = fetchContent(WebPage.newWebPage(url, conf.toVolatileConfig()))
    
    @Throws(Exception::class)
    override fun fetch(url: String, conf: VolatileConfig) = fetchContent(WebPage.newWebPage(url, conf))
    
    /**
     * Fetch page content
     * */
    @Throws(Exception::class)
    override fun fetchContent(page: WebPage): Response = runBlocking {
        fetchContentDeferred(page)
    }
    
    @Throws(Exception::class)
    override suspend fun fetchDeferred(url: String) =
        fetchContentDeferred(WebPage.newWebPage(url, conf.toVolatileConfig()))
    
    @Throws(Exception::class)
    override suspend fun fetchDeferred(url: String, volatileConfig: VolatileConfig) =
        fetchContentDeferred(WebPage.newWebPage(url, volatileConfig))
    
    /**
     * Fetch page content
     * */
    @Throws(Exception::class)
    abstract override suspend fun fetchContentDeferred(page: WebPage): Response
    
    @Throws(Exception::class)
    override suspend fun fetchDeferred(task: FetchTask, driver: WebDriver): FetchResult {
        if (!isActive) {
            return FetchResult.canceled(task, "Browser fetcher is not active")
        }
        
        emit(EventType.willFetch, task.page, driver)

//        val result = try {
//            browserEmulator.visit(task, driver)
//        } catch (e: Throwable) {
//            logger.warn(e.stringify("[Unexpected] Failed to visit page | ${task.url}\n"))
//            FetchResult.failed(task, e)
//        }
        val result = browserEmulator.visit(task, driver)
        
        emit(EventType.fetched, task.page, driver)
        
        return result
    }
    
    private suspend fun emit(type: EventType, page: WebPage, driver: WebDriver) {
        val event = page.browseEventHandlers ?: return
        when(type) {
            EventType.willFetch -> notify(type.name) { event.onWillFetch(page, driver) }
            EventType.fetched -> notify(type.name) { event.onFetched(page, driver) }
            else -> {}
        }
    }
    
    private suspend fun notify(name: String, action: suspend () -> Unit) {
        if (!isActive) {
            return
        }
        
        val e = kotlin.runCatching { action() }.exceptionOrNull()
        
        if (e != null && isActive) {
            handleEventException(name, e)
        }
    }
    
    private fun handleEventException(name: String, e: Throwable) {
        when (e) {
            is WebDriverCancellationException -> logger.info("Web driver is cancelled")
            is WebDriverException -> logger.warn(e.brief("[Ignored][$name] "))
            is Exception -> logger.warn(e.brief("[Ignored][$name] "))
            else -> logger.error(e.stringify("[Unexpected][$name] "))
        }
    }
}
