package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.browser.driver.BrowserSettings
import ai.platon.pulsar.crawl.fetch.privacy.BrowserInstanceId
import ai.platon.pulsar.persist.metadata.BrowserType
import java.io.Closeable
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

interface WebDriver: Closeable {
    val id: Int
    val url: String
    val browserInstanceId: BrowserInstanceId

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
    val currentUrl: String?
    val pageSource: String?
    val delayPolicy: (String) -> Long get() = { 300L + Random.nextInt(500) }

    fun navigateTo(url: String)
    fun setTimeouts(driverConfig: BrowserSettings)

    fun bringToFront()
    fun waitFor(selector: String): Long = 0
    fun exists(selector: String): Boolean = false
    fun type(selector: String, text: String) {}
    fun click(selector: String, count: Int = 1) {}

    fun evaluate(expression: String): Any?
    fun evaluateSilently(expression: String): Any?

    fun stopLoading()

    fun free()
    fun startWork()
    fun retire()
    fun quit()
    fun cancel()
}
