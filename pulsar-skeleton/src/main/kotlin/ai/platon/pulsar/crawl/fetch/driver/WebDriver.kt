package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.common.browser.BrowserType
import org.jsoup.Connection
import java.io.Closeable
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.random.Random

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
data class NavigateEntry(
    val url: String,
    var stopped: Boolean = false,
    var activeTime: Instant = Instant.now(),
    val createTime: Instant = Instant.now(),
) {
    fun refresh() {
        activeTime = Instant.now()
    }
}

interface WebDriver: Closeable {
    val id: Int
    val url: String
    val browserInstance: BrowserInstance
    val browserInstanceId: BrowserInstanceId get() = browserInstance.id

    val lastActiveTime: Instant
    val idleTimeout: Duration
    val isIdle get() = Duration.between(lastActiveTime, Instant.now()) > idleTimeout

    val isCanceled: Boolean
    val isQuit: Boolean
    val isRetired: Boolean

    val name: String
    val browserType: BrowserType
    val supportJavascript: Boolean
    val isMockedPageSource: Boolean
    val sessionId: String?
    val delayPolicy: (String) -> Long get() = { 300L + Random.nextInt(500) }

    suspend fun currentUrl(): String
    suspend fun pageSource(): String?
    suspend fun mainRequestHeaders(): Map<String, Any>
    suspend fun mainRequestCookies(): List<Map<String, String>>
    suspend fun getCookies(): List<Map<String, String>>

    suspend fun navigateTo(url: String)
    suspend fun setTimeouts(browserSettings: BrowserSettings)

    suspend fun bringToFront()
    /**
     * Returns when element specified by selector satisfies {@code state} option.
     * */
    suspend fun waitForSelector(selector: String): Long
    /**
     * Returns when element specified by selector satisfies {@code state} option.
     * Returns the time remaining until timeout
     * */
    suspend fun waitForSelector(selector: String, timeoutMillis: Long): Long
    suspend fun waitForSelector(selector: String, timeout: Duration): Long
    suspend fun waitForNavigation(): Long
    suspend fun waitForNavigation(timeoutMillis: Long): Long
    suspend fun waitForNavigation(timeout: Duration): Long
    suspend fun exists(selector: String): Boolean
    suspend fun type(selector: String, text: String)
    suspend fun click(selector: String, count: Int = 1)
    suspend fun scrollTo(selector: String)
    suspend fun scrollDown(count: Int = 1)
    suspend fun scrollUp(count: Int = 1)

    suspend fun outerHTML(selector: String): String?
    suspend fun firstText(selector: String): String?
    suspend fun allTexts(selector: String): List<String>
    suspend fun firstAttr(selector: String, attrName: String): String?
    suspend fun allAttrs(selector: String, attrName: String): List<String>

    suspend fun evaluate(expression: String): Any?
    suspend fun evaluateSilently(expression: String): Any?

    suspend fun newSession(): Connection
    suspend fun loadResource(url: String): Connection.Response?

    suspend fun stop()

    fun free()
    fun startWork()
    fun retire()
    fun quit()
    fun cancel()
}
