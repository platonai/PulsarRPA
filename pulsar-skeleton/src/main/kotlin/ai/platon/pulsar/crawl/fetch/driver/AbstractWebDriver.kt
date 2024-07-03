package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.browser.driver.chrome.NetworkResourceResponse
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.common.warnForClose
import ai.platon.pulsar.dom.nodes.GeoAnchor
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
import kotlin.random.nextInt

abstract class AbstractWebDriver(
    override val browser: AbstractBrowser,
    override val id: Int = ID_SUPPLIER.incrementAndGet()
): Comparable<AbstractWebDriver>, AbstractJvmWebDriver(), WebDriver, JvmWebDriver {
    companion object {
        private val ID_SUPPLIER = AtomicInteger()
    }

    /**
     * The state of the driver.
     * */
    enum class State {
        /**
         * The driver is initialized.
         * */
        INIT,
        /**
         * The driver is ready to work.
         * */
        READY,
        /**
         * The driver is working.
         * */
        WORKING,
        /**
         * The driver is retired and should be quit as soon as possible.
         * */
        RETIRED,
        /**
         * The driver is quit.
         * */
        QUIT;
        /**
         * Whether the driver is initialized.
         * */
        val isInit get() = this == INIT
        /**
         * Whether the driver is ready to work.
         * */
        val isReady get() = this == READY
        /**
         * Whether the driver is working.
         * */
        val isWorking get() = this == WORKING
        /**
         * Whether the driver is quit.
         * */
        val isQuit get() = this == QUIT
        /**
         * Whether the driver is retired and should be quit as soon as possible.
         * */
        val isRetired get() = this == RETIRED
    }

    /**
     * The state of the driver.
     * * [State.INIT]: The driver is initialized.
     * * [State.READY]: The driver is ready to work.
     * * [State.WORKING]: The driver is working.
     * * [State.RETIRED]: The driver is retired and should be quit as soon as possible.
     * * [State.QUIT]: The driver is quit.
     * */
    private val state = AtomicReference(State.INIT)

    private val canceled = AtomicBoolean()
    private val crashed = AtomicBoolean()

    private val jsoupCreateDestroyMonitor = Any()
    private var jsoupSession: Connection? = null

    var idleTimeout: Duration = Duration.ofMinutes(10)
    var lastActiveTime: Instant = Instant.now()
    /**
     * Whether the driver is idle. The driver is idle if it is not working for a period of time.
     * */
    val isIdle get() = Duration.between(lastActiveTime, Instant.now()) > idleTimeout

    val isInit get() = state.get().isInit
    val isReady get() = state.get().isReady

    val isWorking get() = state.get().isWorking
    val isRetired get() = state.get().isRetired
    val isQuit get() = state.get().isQuit

    val isCanceled get() = canceled.get()
    val isCrashed get() = crashed.get()

    open val supportJavascript: Boolean = true

    open val isMockedPageSource: Boolean = false

    var isRecovered: Boolean = false

    var isReused: Boolean = false

    open val name get() = javaClass.simpleName + "-" + id

    override var navigateEntry: NavigateEntry = NavigateEntry("")

    override val navigateHistory = NavigateHistory()

    override val delayPolicy by lazy { browser.browserSettings.interactSettings.generateRestrictedDelayPolicy() }

    override val timeoutPolicy by lazy { browser.browserSettings.interactSettings.generateRestrictedTimeoutPolicy() }

    override val frames: MutableList<WebDriver> = mutableListOf()

    override var opener: WebDriver? = null

    override val outgoingPages: MutableSet<WebDriver> = mutableSetOf()

    /**
     * The associated data.
     * */
    override val data: MutableMap<String, Any?> = mutableMapOf()

    /**
     * Mark the driver as free, so it can be used to fetch a new page.
     * */
    fun free() {
        canceled.set(false)
        crashed.set(false)
        if (!isInit && !isWorking) {
            // It's a bad idea to throw an exception, which lead to inconsistency within the ConcurrentStatefulDriverPool.
            // throw IllegalWebDriverStateException("The driver is expected to be INIT or WORKING to be ready, actually $state")
        }
        state.set(State.READY)
    }

    /**
     * Mark the driver as working, so it can not be used to fetch another page.
     * */
    fun startWork() {
        canceled.set(false)
        crashed.set(false)
        if (!isInit && !isReady) {
            // It's a bad idea to throw an exception, which lead to inconsistency within the ConcurrentStatefulDriverPool.
            // throw IllegalWebDriverStateException("The driver is expected to be INIT or READY to work, actually $state")
        }
        state.set(State.WORKING)
    }

    /**
     * Mark the driver as retired, so it can not be used to fetch any page,
     * and should be quit as soon as possible.
     * */
    fun retire() = state.set(State.RETIRED)
    /**
     * Mark the driver as canceled, so the fetch process should return as soon as possible,
     * and the fetch result should be dropped.
     * */
    fun cancel() {
        canceled.set(true)
    }

    override fun jvm(): JvmWebDriver = this

    val mainRequestHeaders: Map<String, Any> get() = navigateEntry.mainRequestHeaders
    val mainRequestCookies: List<Map<String, String>> get() = navigateEntry.mainRequestCookies
    val mainResponseStatus: Int get() = navigateEntry.mainResponseStatus
    val mainResponseStatusText: String get() = navigateEntry.mainResponseStatusText
    val mainResponseHeaders: Map<String, Any> get() = navigateEntry.mainResponseHeaders

    @Throws(WebDriverException::class)
    override suspend fun navigateTo(url: String) = navigateTo(NavigateEntry(url))

    override suspend fun currentUrl(): String = evaluate("document.URL", navigateEntry.url)

    @Throws(WebDriverException::class)
    override suspend fun url() = evaluate("document.URL", "")

    @Throws(WebDriverException::class)
    override suspend fun documentURI() = evaluate("document.documentURI", "")

    @Throws(WebDriverException::class)
    override suspend fun baseURI() = evaluate("document.baseURI", "")

    @Throws(WebDriverException::class)
    override suspend fun referrer() = evaluate("document.referrer", "")

    @Suppress("UNCHECKED_CAST")
    @Throws(WebDriverException::class)
    override suspend fun <T> evaluate(expression: String, defaultValue: T): T {
        return evaluate(expression) as? T ?: defaultValue
    }

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

    @Deprecated("Use scrollToMiddle(Double) instead", replaceWith = ReplaceWith("scrollToMiddle(ratio.toDouble())"))
    @Throws(WebDriverException::class)
    override suspend fun scrollToMiddle(ratio: Float) = scrollToMiddle(ratio.toDouble())

    @Throws(WebDriverException::class)
    override suspend fun scrollToMiddle(ratio: Double) {
        evaluate("__pulsar_utils__.scrollToMiddle($ratio)")
    }

    @Throws(WebDriverException::class)
    override suspend fun clickNthAnchor(n: Int, rootSelector: String): String? {
        val result = evaluate("__pulsar_utils__.clickNthAnchor($n, '$rootSelector')")
        return result?.toString()
    }

    @Throws(WebDriverException::class)
    override suspend fun outerHTML() = outerHTML(":root")

    @Throws(WebDriverException::class)
    override suspend fun outerHTML(selector: String): String? {
        val result = evaluate("__pulsar_utils__.outerHTML('$selector')")
        return result?.toString()
    }

    @Throws(WebDriverException::class)
    override suspend fun selectFirstTextOrNull(selector: String): String? {
        val result = evaluate("__pulsar_utils__.selectFirstText('$selector')")
        return result?.toString()
    }

    @Throws(WebDriverException::class)
    override suspend fun selectTextAll(selector: String): List<String> {
        val json = evaluate("__pulsar_utils__.selectTextAll('$selector')")?.toString()?: "[]"
        val result: List<String> = jacksonObjectMapper().readValue(json)
        return result
    }

    @Throws(WebDriverException::class)
    override suspend fun selectFirstAttributeOrNull(selector: String, attrName: String): String? {
        val result = evaluate("__pulsar_utils__.selectFirstAttribute('$selector', '$attrName')")
        return result?.toString()
    }

    override suspend fun selectAttributes(selector: String): Map<String, String> {
        val json = evaluate("__pulsar_utils__.selectAttributes('$selector')")?.toString() ?: return mapOf()
        val attributes: List<String> = jacksonObjectMapper().readValue(json)
        return attributes.zipWithNext().associate { it }
    }

    @Throws(WebDriverException::class)
    override suspend fun selectAttributeAll(selector: String, attrName: String, start: Int, limit: Int): List<String> {
        val json = evaluate("__pulsar_utils__.selectAttributeAll('$selector', '$attrName')")?.toString() ?: return listOf()
        return jacksonObjectMapper().readValue(json)
    }

    @Throws(WebDriverException::class)
    override suspend fun setAttribute(selector: String, attrName: String, attrValue: String) {
        evaluate("__pulsar_utils__.setAttribute('$selector', '$attrName', '$attrValue')")
    }

    @Throws(WebDriverException::class)
    override suspend fun setAttributeAll(selector: String, attrName: String, attrValue: String) {
        evaluate("__pulsar_utils__.setAttributeAll('$selector', '$attrName', '$attrValue')")
    }

    /**
     * Find hyperlinks in elements matching the CSS query.
     * */
    @Throws(WebDriverException::class)
    override suspend fun selectHyperlinks(selector: String, offset: Int, limit: Int): List<Hyperlink> {
        // val result = evaluate("__pulsar_utils__.allAttrs('$selector', 'abs:href')")
        // TODO: add __pulsar_utils__.selectHyperlinks()
        return selectAttributeAll(selector, "abs:href").drop(offset).take(limit).map { Hyperlink(it) }
    }

    /**
     * Find image elements matching the CSS query.
     * */
    @Throws(WebDriverException::class)
    override suspend fun selectAnchors(selector: String, offset: Int, limit: Int): List<GeoAnchor> {
        // TODO: add __pulsar_utils__.selectAnchors()
        return selectAttributeAll(selector, "abs:href").drop(offset).take(limit).map { GeoAnchor(it, "") }
    }

    /**
     * Find image elements matching the CSS query.
     * */
    @Throws(WebDriverException::class)
    override suspend fun selectImages(selector: String, offset: Int, limit: Int): List<String> {
        // TODO: add __pulsar_utils__.selectImages()
        return selectAttributeAll(selector, "abs:src").drop(offset).take(limit)
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

    @Throws(WebDriverException::class)
    override suspend fun waitForSelector(selector: String) = waitForSelector(selector, timeout("waitForSelector"))

    @Throws(WebDriverException::class)
    override suspend fun waitForSelector(selector: String, action: suspend () -> Unit) =
        waitForSelector(selector, timeout("waitForSelector"), action)

    @Throws(WebDriverException::class)
    override suspend fun waitUntil(predicate: suspend () -> Boolean) = waitUntil(timeout("waitUntil"), predicate)

    @Throws(WebDriverException::class)
    override suspend fun waitForNavigation(oldUrl: String) = waitForNavigation(oldUrl, timeout("waitForNavigation"))

    @Throws(WebDriverException::class)
    override suspend fun newJsoupSession(): Connection {
        val headers = mainRequestHeaders.entries.associate { it.key to it.value.toString() }
        val cookies = getCookies()

        return newSession(headers, cookies)
    }

    @Throws(IOException::class)
    override suspend fun loadJsoupResource(url: String): Connection.Response {
        val jsession: Connection = synchronized(jsoupCreateDestroyMonitor) {
            jsoupSession ?: createJsoupSession()
        }
        jsoupSession = jsession

        return withContext(Dispatchers.IO) {
            jsession.newRequest().url(url).execute()
        }
    }

    private fun createJsoupSession(): Connection {
        val (headers, cookies) = getHeadersAndCookies()
        return newSession(headers, cookies)
    }

    @Throws(IOException::class)
    override suspend fun loadResource(url: String): NetworkResourceResponse {
        return fromJsoup(loadJsoupResource(url))
    }

    override fun equals(other: Any?): Boolean = this === other || (other is AbstractWebDriver && other.id == this.id)

    override fun hashCode(): Int = id

    override fun compareTo(other: AbstractWebDriver): Int = id - other.id

    override fun toString(): String = "#$id"
    /**
     * Force the page stop all navigations and RELEASES all resources.
     * If a web driver is terminated, it should not be used any more and should be quit
     * as soon as possible.
     * */
    @Throws(WebDriverException::class)
    open suspend fun terminate() {
        stop()
    }

    /** Wait until the tab is terminated and closed. */
    @Throws(Exception::class)
    open fun awaitTermination() {

    }

    override fun close() {
        if (isQuit) {
            return
        }

        state.set(State.QUIT)
        runCatching { runBlocking { stop() } }.onFailure { warnForClose(this, it) }
    }

    /**
     * Generate a random delay in milliseconds.
     *
     * The generated delay time is an int random value uniformly distributed in a specified range.
     *
     * The delay range should be in [1, 10000], and the default range is [500, 1000].
     * */
    fun randomDelayMillis(action: String, fallback: IntRange = 500..1000): Long {
        val default = delayPolicy["default"] ?: fallback
        var range = delayPolicy[action] ?: default

        if (range.first <= 0 || range.last > 10000) {
            range = fallback
        }

        return Random.nextInt(range).toLong()
    }

    fun timeout(action: String, fallback: Duration = Duration.ofSeconds(60)): Duration {
        return timeoutPolicy[action] ?: timeoutPolicy["default"] ?: timeoutPolicy[""] ?: fallback
    }

    private fun getHeadersAndCookies(): Pair<Map<String, String>, List<Map<String, String>>> {
        return runBlocking {
            val headers = mainRequestHeaders.entries.associate { it.key to it.value.toString() }
            val cookies = getCookies()

            headers to cookies
        }
    }

    /**
     * Create a new session with the same context of the browser: headers, cookies, proxy, etc.
     * The browser should be initialized by opening a page before the session is created.
     * */
    private fun newSession(headers: Map<String, String>, cookies: List<Map<String, String>>): Connection {
        val httpTimeout = Duration.ofSeconds(20)
        val session = Jsoup.newSession()
            .timeout(httpTimeout.toMillis().toInt())
            .headers(headers)
            .ignoreContentType(true)
            .ignoreHttpErrors(true)

        session.userAgent(browser.userAgent)

        if (cookies.isNotEmpty()) {
            session.cookies(cookies.first())
        }

        // Since the browser uses the system proxy (by default),
        // so the http connection should also use the system proxy
        val proxy = browser.id.fingerprint.proxyServer ?: System.getenv("http_proxy")
        if (proxy != null && UrlUtils.isStandard(proxy)) {
            val u = UrlUtils.getURLOrNull(proxy)
            if (u != null) {
                // TODO: sock proxy support
                session.proxy(u.host, u.port)
            }
        }

        return session
    }

    fun fromJsoup(response: Connection.Response): NetworkResourceResponse {
        val success = response.statusCode() == 200
        val httpStatusCode = response.statusCode()
        //    val stream = response.bodyStream()
        val stream = response.body()
        val headers = response.headers().toMutableMap()
        // All pulsar added headers have a prefix Q-
        headers["Q-client"] = "Jsoup"
        return NetworkResourceResponse(success, 0, "", httpStatusCode, stream, headers)
    }
}
