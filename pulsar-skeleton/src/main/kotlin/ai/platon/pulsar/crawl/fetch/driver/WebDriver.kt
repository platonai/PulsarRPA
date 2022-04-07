package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.driver.chrome.common.ChromeOptions
import ai.platon.pulsar.browser.driver.chrome.common.LauncherOptions
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.persist.metadata.BrowserType
import org.jsoup.Connection
import java.io.Closeable
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Condition
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

abstract class AbstractBrowserInstance(
    val id: BrowserInstanceId,
    val launcherOptions: LauncherOptions,
    val launchOptions: ChromeOptions
): AutoCloseable {
    val isGUI get() = launcherOptions.browserSettings.isGUI

    var tabCount = AtomicInteger()
    // remember, navigate history is small, so search is very fast for a list
    val navigateHistory = mutableListOf<NavigateEntry>()
    var activeTime = Instant.now()
    val idleTimeout = Duration.ofMinutes(10)
    val isIdle get() = Duration.between(activeTime, Instant.now()) > idleTimeout

    private val initializedLock = ReentrantLock()
    private val initialized = initializedLock.newCondition()

    protected val launched = AtomicBoolean()
    protected val closed = AtomicBoolean()

    abstract fun launch()

    fun await() {
        initializedLock.withLock { initialized.await() }
    }

    fun signalAll() {
        initializedLock.withLock { initialized.signalAll() }
    }
}

interface WebDriver: Closeable {
    val id: Int
    val url: String
    val browserInstance: AbstractBrowserInstance
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

    suspend fun navigateTo(url: String)
    suspend fun setTimeouts(driverConfig: BrowserSettings)

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

    suspend fun getCookies(): List<Map<String, String>>
    suspend fun evaluate(expression: String): Any?
    suspend fun evaluateSilently(expression: String): Any?

    suspend fun newSession(): Connection

    suspend fun stop()

    fun free()
    fun startWork()
    fun retire()
    fun quit()
    fun cancel()
}
