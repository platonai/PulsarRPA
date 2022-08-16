package ai.platon.pulsar.crawl.fetch.driver

import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.common.urls.UrlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

abstract class AbstractWebDriver(
    override val browserInstance: BrowserInstance,
    override val id: Int = 0
): Comparable<AbstractWebDriver>, WebDriver {

    enum class Status {
        UNKNOWN, FREE, WORKING, CANCELED, RETIRED, CRASHED, QUIT;

        val isFree get() = this == FREE
        val isWorking get() = this == WORKING
        val isCanceled get() = this == CANCELED
        val isRetired get() = this == RETIRED
        val isCrashed get() = this == CRASHED
        val isQuit get() = this == QUIT
    }

    var waitForTimeout = Duration.ofMinutes(1)

    override var idleTimeout: Duration = Duration.ofMinutes(10)

    override val name get() = javaClass.simpleName + "-" + id

    /**
     * The url to navigate
     * The browser might redirect, so it might not be the same with currentUrl()
     * */
    override var navigateEntry: NavigateEntry = NavigateEntry("")

    /**
     * The url to navigate
     * The browser might redirect, so it might not be the same with currentUrl()
     * */
//    override var url: String = navigateEntry.url
    /**
     * Whether the web driver has javascript support
     * */
    override val supportJavascript: Boolean = true
    /**
     * Whether the web page source is mocked
     * */
    override val isMockedPageSource: Boolean = false
    /**
     * Driver status
     * */
    val status = AtomicReference(Status.UNKNOWN)

    override var lastActiveTime: Instant = Instant.now()

    val isFree get() = status.get().isFree
    val isWorking get() = status.get().isWorking
    val isNotWorking get() = !isWorking
    val isCrashed get() = status.get().isCrashed
    override val isRetired get() = status.get().isRetired
    override val isCanceled get() = status.get().isCanceled
    override val isQuit get() = status.get().isQuit

    private var jsoupSession: Connection? = null

    override fun free() = status.set(Status.FREE)
    override fun startWork() = status.set(Status.WORKING)
    override fun retire() = status.set(Status.RETIRED)
    override fun cancel() {
        if (isCanceled) {
            return
        }

        if (status.compareAndSet(Status.WORKING, Status.CANCELED)) {
            // stop()
        }
    }

    override suspend fun navigateTo(url: String) = navigateTo(NavigateEntry(url))

    override suspend fun waitForSelector(selector: String, timeoutMillis: Long): Long =
        waitForSelector(selector, Duration.ofMillis(timeoutMillis))
    override suspend fun waitForSelector(selector: String): Long = waitForSelector(selector, waitForTimeout)

    override suspend fun waitForNavigation(): Long = waitForNavigation(Duration.ofSeconds(10))
    override suspend fun waitForNavigation(timeoutMillis: Long): Long = waitForNavigation(Duration.ofMillis(timeoutMillis))

    override suspend fun evaluateSilently(expression: String): Any? =
        takeIf { isWorking }?.runCatching { evaluate(expression) }

    override suspend fun scrollDown(count: Int) {
        evaluate("__pulsar_utils__.scrollDown()")
    }

    override suspend fun scrollUp(count: Int) {
        evaluate("__pulsar_utils__.scrollUp()")
    }

    override suspend fun outerHTML(selector: String): String? {
        val result = evaluate("__pulsar_utils__.outerHTML('$selector')")
        return result?.toString()
    }

    override suspend fun firstText(selector: String): String? {
        val result = evaluate("__pulsar_utils__.firstText('$selector')")
        return result?.toString()
    }

    override suspend fun allTexts(selector: String): List<String> {
        val result = evaluate("__pulsar_utils__.allTexts('$selector')")
        return result?.toString()?.split("\n")?.toList() ?: listOf()
    }

    override suspend fun firstAttr(selector: String, attrName: String): String? {
        val result = evaluate("__pulsar_utils__.firstAttr('$selector', '$attrName')")
        return result?.toString()
    }

    override suspend fun allAttrs(selector: String, attrName: String): List<String> {
        val result = evaluate("__pulsar_utils__.allAttrs('$selector', '$attrName')")
        return result?.toString()?.split("\n")?.toList() ?: listOf()
    }

    /**
     * Create a new session with the same context of the browser: headers, cookies, proxy, etc.
     * The browser should be initialized by opening a page before the session is created.
     * */
    override suspend fun newSession(): Connection {
        val headers = mainRequestHeaders().entries.associate { it.key to it.value.toString() }
        val cookies = getCookies()
        val userAgent = BrowserSettings.randomUserAgent()

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
        val proxy = browserInstance.id.proxyServer ?: System.getenv("http_proxy")
        if (proxy != null && UrlUtils.isValidUrl(proxy)) {
            val u = URL(proxy)
            session.proxy(u.host, u.port)
        }

        return session
    }

    @Throws(IOException::class)
    override suspend fun loadResource(url: String): Connection.Response? {
        if (jsoupSession == null) {
            jsoupSession = newSession()
        }

        val response = withContext(Dispatchers.IO) {
            jsoupSession?.newRequest()?.url(url)?.execute()
        }

        return response
    }

    override fun equals(other: Any?): Boolean = other is AbstractWebDriver && other.id == this.id

    override fun hashCode(): Int = id

    override fun compareTo(other: AbstractWebDriver): Int = id - other.id

    override fun toString(): String = sessionId?.let { "#$id-$sessionId" }?:"#$id(closed)"
}
