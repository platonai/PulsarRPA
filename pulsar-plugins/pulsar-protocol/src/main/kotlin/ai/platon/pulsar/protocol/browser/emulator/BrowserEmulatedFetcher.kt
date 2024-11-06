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

import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.PrivacyManager
import ai.platon.pulsar.skeleton.crawl.protocol.Response
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2023 Platon AI. All rights reserved
 */
interface BrowserEmulatedFetcher: AutoCloseable {

    val privacyManager: PrivacyManager
    val driverPoolManager: WebDriverPoolManager
    val browserEmulator: BrowserEmulator
    
    @Throws(Exception::class)
    fun fetch(url: String): Response
    
    @Throws(Exception::class)
    fun fetch(url: String, conf: VolatileConfig): Response

    /**
     * Fetch page content.
     *
     * @param page the page to fetch
     * @return the response
     * */
    @Throws(Exception::class)
    fun fetchContent(page: WebPage): Response
    
    /**
     * Fetch a url.
     *
     * @param url the url to fetch
     * @return the response
     * */
    @Throws(Exception::class)
    suspend fun fetchDeferred(url: String): Response
    
    /**
     * Fetch a url.
     *
     * @param url the url to fetch
     * @return the response
     * */
    @Throws(Exception::class)
    suspend fun fetchDeferred(url: String, volatileConfig: VolatileConfig): Response
    
    /**
     * Fetch page content.
     *
     * @param page the page to fetch
     * @return the response
     * */
    @Throws(Exception::class)
    suspend fun fetchContentDeferred(page: WebPage): Response

    fun reset()

    fun cancel(page: WebPage)

    fun cancelAll()
}
