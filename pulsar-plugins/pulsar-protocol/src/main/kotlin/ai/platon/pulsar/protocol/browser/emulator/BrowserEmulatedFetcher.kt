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
