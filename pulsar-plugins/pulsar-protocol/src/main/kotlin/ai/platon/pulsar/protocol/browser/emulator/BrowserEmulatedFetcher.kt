package ai.platon.pulsar.protocol.browser.emulator

import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.crawl.fetch.privacy.PrivacyManager
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.protocol.browser.driver.WebDriverPoolManager

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
interface BrowserEmulatedFetcher: AutoCloseable {

    val privacyManager: PrivacyManager
    val driverPoolManager: WebDriverPoolManager
    val browserEmulator: BrowserEmulator

    fun fetch(url: String): Response

    fun fetch(url: String, conf: VolatileConfig): Response

    /**
     * Fetch page content
     * */
    fun fetchContent(page: WebPage): Response

    suspend fun fetchDeferred(url: String): Response

    suspend fun fetchDeferred(url: String, volatileConfig: VolatileConfig): Response

    /**
     * Fetch page content
     * */
    suspend fun fetchContentDeferred(page: WebPage): Response

    fun reset()

    fun cancel(page: WebPage)

    fun cancelAll()
}
