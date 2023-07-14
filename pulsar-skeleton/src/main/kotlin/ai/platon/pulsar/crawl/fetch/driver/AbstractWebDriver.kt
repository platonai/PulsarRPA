package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.common.urls.UrlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

abstract class AbstractWebDriver(
    override val browser: Browser,
    override val id: Int = instanceSequencer.incrementAndGet()
): Comparable<AbstractWebDriver>, AbstractJvmWebDriver(), WebDriver, JvmWebDriver {
    companion object {
        val instanceSequencer = AtomicInteger()
    }

    private val jsoupCreateDestroyMonitor = Any()
    private var jsoupSession: Connection? = null
    private val canceled = AtomicBoolean()
    private val crashed = AtomicBoolean()

    override var idleTimeout: Duration = Duration.ofMinutes(10)

    override var waitForElementTimeout = Duration.ofSeconds(20)

    override val name get() = javaClass.simpleName + "-" + id

    override val delayPolicy: (String) -> Long get() = { type ->
        when (type) {
            "gap" -> 500L + Random.nextInt(500)
            "click" -> 500L + Random.nextInt(1000)
            "type" -> 50L + Random.nextInt(500)
            "mouseWheel" -> 800L + Random.nextInt(500)
            "dragAndDrop" -> 800L + Random.nextInt(500)
            "waitForNavigation" -> 500L
            "waitForSelector" -> 1000L
            else -> 100L + Random.nextInt(500)
        }
    }

    override var navigateEntry: NavigateEntry = NavigateEntry("")

    override val navigateHistory = NavigateHistory()

    override val supportJavascript: Boolean = true

    override val isMockedPageSource: Boolean = false

    override var isRecovered: Boolean = false

    override var isReused: Boolean = false

    override val state = AtomicReference(WebDriver.State.INIT)

    /**
     * The associated data.
     * */
    override val data: MutableMap<String, Any?> = mutableMapOf()

    override var lastActiveTime: Instant = Instant.now()

    override val isInit get() = state.get().isInit
    override val isReady get() = state.get().isReady
    @Deprecated("Inappropriate name", replaceWith = ReplaceWith("isReady()"))
    override val isFree get() = isReady
    override val isWorking get() = state.get().isWorking
    override val isRetired get() = state.get().isRetired
    override val isQuit get() = state.get().isQuit

    override val isCanceled get() = canceled.get()
    override val isCrashed get() = crashed.get()

    override fun free() {
        canceled.set(false)
        crashed.set(false)
        if (!isInit && !isWorking) {
            throw IllegalStateException("A driver has to be ready before work, actual $state")
        }
        state.set(WebDriver.State.READY)
    }
    override fun startWork() {
        canceled.set(false)
        crashed.set(false)
        if (!isInit && !isReady) {
            throw IllegalStateException("A driver has to be ready before work, actual $state")
        }
        state.set(WebDriver.State.WORKING)
    }
    override fun retire() = state.set(WebDriver.State.RETIRED)
    override fun cancel() {
        canceled.set(true)
    }

    override fun jvm(): JvmWebDriver = this

    @Deprecated("Not used any more", ReplaceWith("id.toString()"))
    override val sessionId: String?
        get() = id.toString()

    override val mainRequestHeaders: Map<String, Any> get() = navigateEntry.mainRequestHeaders
    override val mainRequestCookies: List<Map<String, String>> get() = navigateEntry.mainRequestCookies
    override val mainResponseStatus: Int get() = navigateEntry.mainResponseStatus
    override val mainResponseStatusText: String get() = navigateEntry.mainResponseStatusText
    override val mainResponseHeaders: Map<String, Any> get() = navigateEntry.mainResponseHeaders

    @Throws(WebDriverException::class)
    override suspend fun navigateTo(url: String) = navigateTo(NavigateEntry(url))

    override suspend fun location(): String {
        val result = evaluate("window.location")
        return result?.toString() ?: ""
    }

    override suspend fun baseURI(): String {
        val result = evaluate("document.baseURI")
        return result?.toString() ?: ""
    }

    @Throws(WebDriverException::class)
    override suspend fun waitForSelector(selector: String, timeoutMillis: Long): Long =
        waitForSelector(selector, Duration.ofMillis(timeoutMillis))

    @Throws(WebDriverException::class)
    override suspend fun waitForSelector(selector: String): Long = waitForSelector(selector, waitForElementTimeout)

    @Throws(WebDriverException::class)
    override suspend fun waitForNavigation(): Long = waitForNavigation(Duration.ofSeconds(10))

    @Throws(WebDriverException::class)
    override suspend fun waitForNavigation(timeoutMillis: Long): Long = waitForNavigation(Duration.ofMillis(timeoutMillis))

    override suspend fun evaluateSilently(expression: String): Any? =
        takeIf { isWorking }?.runCatching { evaluate(expression) }

    @Throws(WebDriverException::class)
    override suspend fun isVisible(selector: String): Boolean {
        return evaluate("__pulsar_utils__.isVisible('$selector')") == "true"
    }

    override suspend fun isChecked(selector: String): Boolean {
        return evaluate("__pulsar_utils__.isChecked('$selector')") == "true"
    }

    @Throws(WebDriverException::class)
    override suspend fun scrollDown(count: Int) {
        repeat(count) {
            evaluate("__pulsar_utils__.scrollDown()")
        }
    }

    @Throws(WebDriverException::class)
    override suspend fun scrollUp(count: Int) {
        evaluate("__pulsar_utils__.scrollUp()")
    }

    @Throws(WebDriverException::class)
    override suspend fun scrollToTop() {
        evaluate("__pulsar_utils__.scrollToTop()")
    }

    @Throws(WebDriverException::class)
    override suspend fun scrollToBottom() {
        evaluate("__pulsar_utils__.scrollToBottom()")
    }

    @Throws(WebDriverException::class)
    override suspend fun scrollToMiddle(ratio: Float) {
        evaluate("__pulsar_utils__.scrollToMiddle($ratio)")
    }

    @Throws(WebDriverException::class)
    override suspend fun clickNthAnchor(n: Int, rootSelector: String): String? {
        val result = evaluate("__pulsar_utils__.clickNthAnchor($n, '$rootSelector')")
        return result?.toString()
    }

    @Throws(WebDriverException::class)
    override suspend fun outerHTML(selector: String): String? {
        val result = evaluate("__pulsar_utils__.outerHTML('$selector')")
        return result?.toString()
    }

    @Throws(WebDriverException::class)
    override suspend fun firstText(selector: String): String? {
        val result = evaluate("__pulsar_utils__.firstText('$selector')")
        return result?.toString()
    }

    @Throws(WebDriverException::class)
    override suspend fun allTexts(selector: String): List<String> {
        val result = evaluate("__pulsar_utils__.allTexts('$selector')")
        return result?.toString()?.split("\n")?.toList() ?: listOf()
    }

    @Throws(WebDriverException::class)
    override suspend fun firstAttr(selector: String, attrName: String): String? {
        val result = evaluate("__pulsar_utils__.firstAttr('$selector', '$attrName')")
        return result?.toString()
    }

    @Throws(WebDriverException::class)
    override suspend fun allAttrs(selector: String, attrName: String): List<String> {
        val result = evaluate("__pulsar_utils__.allAttrs('$selector', '$attrName')")
        return result?.toString()?.split("\n")?.toList() ?: listOf()
    }

    @Throws(WebDriverException::class)
    override suspend fun clickTextMatches(selector: String, pattern: String, count: Int) {
        evaluate("__pulsar_utils__.clickTextMatches('$selector', '$pattern')")
    }

    @Throws(WebDriverException::class)
    override suspend fun clickMatches(selector: String, attrName: String, pattern: String, count: Int) {
        evaluate("__pulsar_utils__.clickMatches('$selector', '$attrName', '$pattern')")
    }

    @Throws(WebDriverException::class)
    override suspend fun check(selector: String) {
        evaluate("__pulsar_utils__.check('$selector')")
    }

    @Throws(WebDriverException::class)
    override suspend fun uncheck(selector: String) {
        evaluate("__pulsar_utils__.uncheck('$selector')")
    }

    /**
     * Create a new session with the same context of the browser: headers, cookies, proxy, etc.
     * The browser should be initialized by opening a page before the session is created.
     * */
    @Throws(WebDriverException::class)
    override suspend fun newSession(): Connection {
        val headers = mainRequestHeaders().entries.associate { it.key to it.value.toString() }
        val cookies = getCookies()

        return newSession(headers, cookies)
    }

    @Throws(IOException::class)
    override suspend fun loadResource(url: String): Connection.Response? {
        synchronized(jsoupCreateDestroyMonitor) {
            if (jsoupSession == null) {
                val (headers, cookies) = getHeadersAndCookies()
                jsoupSession = newSession(headers, cookies)
            }
        }

        val response = withContext(Dispatchers.IO) {
            jsoupSession?.newRequest()?.url(url)?.execute()
        }

        return response
    }

    /**
     * Quit the browser instance
     * */
    override fun quit() {
        close()
    }

    override fun equals(other: Any?): Boolean = this === other || (other is AbstractWebDriver && other.id == this.id)

    override fun hashCode(): Int = id

    override fun compareTo(other: AbstractWebDriver): Int = id - other.id

    override fun toString(): String = sessionId?.let { "#$id-$sessionId" }?:"#$id"

    private fun getHeadersAndCookies(): Pair<Map<String, String>, List<Map<String, String>>> {
        return runBlocking {
            val headers = mainRequestHeaders().entries.associate { it.key to it.value.toString() }
            val cookies = getCookies()

            headers to cookies
        }
    }

    /**
     * Create a new session with the same context of the browser: headers, cookies, proxy, etc.
     * The browser should be initialized by opening a page before the session is created.
     * */
    private fun newSession(headers: Map<String, String>, cookies: List<Map<String, String>>): Connection {
        // TODO: use the same user agent as this browser
        val userAgent = browser.userAgent ?: (browser as AbstractBrowser).browserSettings.userAgent.getRandomUserAgent()

        val httpTimeout = Duration.ofSeconds(20)
        val session = Jsoup.newSession()
            .timeout(httpTimeout.toMillis().toInt())
            .userAgent(userAgent)
            .headers(headers)
            .ignoreContentType(true)
            .ignoreHttpErrors(true)

        if (cookies.isNotEmpty()) {
            session.cookies(cookies.first())
        }

        // Since the browser uses the system proxy (by default),
        // so the http connection should also use the system proxy
        val proxy = browser.id.proxyServer ?: System.getenv("http_proxy")
        if (proxy != null && UrlUtils.isStandard(proxy)) {
            val u = UrlUtils.getURLOrNull(proxy)
            if (u != null) {
                session.proxy(u.host, u.port)
            }
        }

        return session
    }
}
