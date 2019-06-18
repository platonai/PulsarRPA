package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.HttpHeaders.*
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.common.config.Params
import ai.platon.pulsar.crawl.protocol.ForwardingResponse
import ai.platon.pulsar.crawl.protocol.Response
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.persist.metadata.MultiMetadata
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import com.google.common.collect.Iterables
import com.udojava.evalex.Expression.e
import org.apache.commons.lang3.StringUtils
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.OutputType
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.ExpectedCondition
import org.openqa.selenium.support.ui.FluentWait
import org.openqa.selenium.support.ui.WebDriverWait
import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import java.time.Duration
import java.time.LocalDateTime
import java.time.MonthDay
import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern.CASE_INSENSITIVE
import kotlin.NoSuchElementException

data class DriverConfig(
        var pageLoadTimeout: Duration,
        var scriptTimeout: Duration,
        var scrollDownCount: Int,
        var scrollDownWait: Duration
) {
    constructor(config: ImmutableConfig): this(
            config.getDuration(FETCH_PAGE_LOAD_TIMEOUT, Duration.ofSeconds(30)),
            config.getDuration(FETCH_SCRIPT_TIMEOUT, Duration.ofSeconds(30)),
            config.getInt(FETCH_SCROLL_DOWN_COUNT, 1),
            config.getDuration(FETCH_SCROLL_DOWN_COUNT, Duration.ofMillis(500))
    )
}

/**
 * Created by vincent on 18-1-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 *
 * Note: SeleniumEngine should be process scope
 */
class SeleniumEngine(val immutableConfig: ImmutableConfig): Parameterized, AutoCloseable {

    private val executor = GlobalExecutor.getInstance(immutableConfig)
    private val drivers = WebDriverQueues(browserControl, immutableConfig)

    private val supportAllCharsets get() = immutableConfig.getBoolean(PARSE_SUPPORT_ALL_CHARSETS, false)
    private var charsetPattern = if (supportAllCharsets) systemAvailableCharsetPattern else defaultCharsetPattern
    private val defaultDriverConfig = DriverConfig(immutableConfig)
    private var libJs = browserControl.parseLibJs(true)
    private var clientJs = browserControl.parseJs(true)

    private val monthDay = DateTimeUtil.now("MMdd")
    private val totalTaskCount = AtomicInteger(0)
    private val totalSuccessCount = AtomicInteger(0)

    // TODO: associate by batch id
    private val batchTaskCount = AtomicInteger(0)
    private val batchSuccessCount = AtomicInteger(0)

    private val isClosed = AtomicBoolean(false)

    init {
        instanceCount.incrementAndGet()
        // log.debug("Get #{}th selenium engine instance" + conf.hashCode() + "\t" + engine.hashCode())
        params.withLogger(log).info()
    }

    override fun getParams(): Params {
        return Params.of(
                "charsetPattern", charsetPattern,
                "pageLoadTimeout", defaultDriverConfig.pageLoadTimeout,
                "scriptTimeout", defaultDriverConfig.scriptTimeout,
                "scrollDownCount", defaultDriverConfig.scrollDownCount,
                "scrollDownWait", defaultDriverConfig.scrollDownWait,
                "clientJsLength", clientJs.length
        )
    }

    fun fetch(url: String): Response {
        return fetchContent(WebPage.newWebPage(url, false))
    }

    fun fetch(url: String, mutableConfig: MutableConfig): Response {
        return fetchContent(WebPage.newWebPage(url, false, mutableConfig))
    }

    /**
     * Fetch page content
     * */
    fun fetchContent(page: WebPage): Response {
        val conf = page.mutableConfig ?: immutableConfig
        val priority = conf.getUint(SELENIUM_WEB_DRIVER_PRIORITY, 0)
        return fetchContentInternal(priority, page, conf)
    }

    fun fetchAll(batchName: String, urls: Iterable<String>): Collection<Response> {
        batchTaskCount.set(0)
        batchSuccessCount.set(0)

        return urls.map { this.fetch(it) }
    }

    fun fetchAll(urls: Iterable<String>): Collection<Response> {
        val batchName = batchTaskId.incrementAndGet().toString()
        return fetchAll(batchName, urls)
    }

    fun fetchAll(batchName: String, urls: Iterable<String>, mutableConfig: MutableConfig): Collection<Response> {
        batchTaskCount.set(0)
        batchSuccessCount.set(0)

        return urls.map { fetch(it, mutableConfig) }
    }

    fun fetchAll(urls: Iterable<String>, mutableConfig: MutableConfig): Collection<Response> {
        val batchName = batchTaskId.incrementAndGet().toString()
        return fetchAll(batchName, urls, mutableConfig)
    }

    fun parallelFetchAll(urls: Iterable<String>, mutableConfig: MutableConfig): Collection<Response> {
        return parallelFetchAllPages(urls.map { WebPage.newWebPage(it) }, mutableConfig)
    }

    fun parallelFetchAll(batchName: String, urls: Iterable<String>, mutableConfig: MutableConfig): Collection<Response> {
        return parallelFetchAllPages(batchName, urls.map { WebPage.newWebPage(it) }, mutableConfig)
    }

    fun parallelFetchAllPages(pages: Iterable<WebPage>, mutableConfig: MutableConfig): Collection<Response> {
        val batchName = batchTaskId.incrementAndGet().toString()
        return parallelFetchAllPages("#$batchName", pages, mutableConfig)
    }

    fun parallelFetchAllPages(batchName: String, pages: Iterable<WebPage>, mutableConfig: MutableConfig): Collection<Response> {
        val size = Iterables.size(pages)

        log.info("Selenium batch task {}: fetching {} pages in parallel", batchName, size)

        batchTaskCount.set(0)
        batchSuccessCount.set(0)

        val priority = mutableConfig.getUint(SELENIUM_WEB_DRIVER_PRIORITY, 0)!!

        // Create a task submitter
        val submitter = { page: WebPage ->
            executor.submit<Response> { fetchContentInternal(priority, page, mutableConfig) }
        }
        // Submit all tasks
        val pendingTasks = pages.associateTo(HashMap()) { it.url to submitter(it) }
        val finishedTasks = mutableMapOf<String, Response>()

        // The function must return in a reasonable time
        val threadTimeout = defaultDriverConfig.pageLoadTimeout.plusSeconds(10)
        val numTotalTasks = pendingTasks.size
        val wait = Duration.ofSeconds(1)
        val idleTimeout = Duration.ofMinutes(2).seconds
        val numAllowedFailures = Math.max(10, numTotalTasks / 3)

        var numFinishedTasks = 0
        var numFailedTasks = 0
        var idleSeconds = 0

        // since the urls in the batch are usually from the same domain,
        // if there are too many failure, the rest tasks are very likely run to failure too
        var i = 0
        while (numFinishedTasks < numTotalTasks && numFailedTasks <= numAllowedFailures && idleSeconds < idleTimeout) {
            if (++i > 1) {
                try { TimeUnit.SECONDS.sleep(wait.seconds) } catch (e: InterruptedException) {
                    log.warn("Selenium interrupted. {}", e)
                    break
                }
            }

            if (i >= 60 && i % 30 == 0) {
                // report every 30 round if it takes long time
                log.warn("Batch {}: round #{}, {} pending, {} finished, {} failed, idle: {}s, idle timeout: {}s",
                        batchName, i, pendingTasks.size, finishedTasks.size, numFailedTasks, idleSeconds, idleTimeout)
            }

            // loop and wait for all parallel tasks return
            var numTaskDone = 0
            val removal = mutableListOf<String>()
            pendingTasks.asSequence().filter { it.value.isDone }.forEach { (key, future) ->
                try {
                    val response = getResponse(key, future, threadTimeout)
                    if (response.code !in arrayOf(ProtocolStatusCodes.SUCCESS_OK, ProtocolStatusCodes.CANCELED)) {
                        ++numFailedTasks
                    }
                    finishedTasks[key] = response
                } catch (e: Throwable) {
                    log.error("Unexpected error {}", StringUtil.stringifyException(e))
                } finally {
                    ++numFinishedTasks
                    ++numTaskDone
                    removal.add(key)
                }
            }
            removal.forEach { pendingTasks.remove(it) }

            idleSeconds = if (numTaskDone == 0) 1 + idleSeconds else 0
        }

        if (pendingTasks.isNotEmpty()) {
            log.warn("Batch task is incomplete, finished tasks {}, pending tasks {}, failed tasks: {}, idle: {}s",
                    finishedTasks.size, pendingTasks.size, numFailedTasks, idleSeconds)

            // if there are still pending tasks, cancel them
            pendingTasks.forEach { it.value.cancel(true) }
            pendingTasks.forEach { url, task ->
                // Attempts to cancel execution of this task
                try {
                    val response = getResponse(url, task, threadTimeout)
                    finishedTasks[url] = response
                } catch (e: Throwable) {
                    log.error("Unexpected error {}", StringUtil.stringifyException(e))
                }
            }
        }

        return finishedTasks.values
    }

    private fun fetchContentInternal(page: WebPage): Response {
        val config = page.mutableConfig ?: immutableConfig
        val priority = config.getUint(SELENIUM_WEB_DRIVER_PRIORITY, 0)
        return fetchContentInternal(priority, page, config)
    }

    /**
     * Must be thread safe
     */
    private fun fetchContentInternal(priority: Int, page: WebPage, config: ImmutableConfig): Response {
        val driver = drivers.poll(priority, config)
                ?: return ForwardingResponse(page.url, ProtocolStatusCodes.RETRY, MultiMetadata())

        totalTaskCount.getAndIncrement()
        batchTaskCount.getAndIncrement()

        // page.baseUrl is the last working address, and page.url is the permanent internal address
        var location = page.baseUrl
        if (location.isBlank()) {
            location = page.url
        }
        val driverConfig = getDriverConfig(priority, page, config)
        var status: ProtocolStatus
        val headers = MultiMetadata()
        val startTime = System.currentTimeMillis()
        headers.put(Q_REQUEST_TIME, startTime.toString())

        var pageSource = ""

        try {
            visit(location, page, driver, driverConfig)
            pageSource = driver.pageSource
            status = ProtocolStatus.STATUS_SUCCESS
            // TODO: handle with frames
            // driver.switchTo().frame(1);
        } catch (e: org.openqa.selenium.TimeoutException) {
            // log.warn(e.toString())
            pageSource = getPageSourceSlient(driver)
            handleWebDriverTimeout(page.url, startTime, pageSource, driverConfig)
            // The javascript set data-error flag to indicate if the vision information of all DOM nodes are calculated
            status = if (isJsSuccess(pageSource)) {
                ProtocolStatus.STATUS_SUCCESS
            } else {
                ProtocolStatus.failed(ProtocolStatusCodes.WEB_DRIVER_TIMEOUT)
            }
        } catch (e: org.openqa.selenium.NoSuchElementException) {
            // failed to wait for body
            status = ProtocolStatus.failed(ProtocolStatusCodes.EXCEPTION)
            log.warn(e.toString())
        } catch (e: org.openqa.selenium.WebDriverException) {
            status = ProtocolStatus.failed(ProtocolStatusCodes.EXCEPTION)
            log.warn(e.toString())
        } catch (e: Throwable) {
            // must not throw again
            status = ProtocolStatus.failed(ProtocolStatusCodes.EXCEPTION)
            log.warn("Unexpected exception: {}", e)
        }

        try {
            handleFetchFinish(page, driver, headers)
            pageSource = handlePageSource(pageSource, status, page, driver)
            headers.put(CONTENT_LENGTH, pageSource.length.toString())
            if (status.isSuccess) {
                handleFetchSuccess(page, driver)
            }
        } finally {
            drivers.put(priority, driver)
        }

        // TODO: handle redirect
        // TODO: collect response header
        // TODO: fetch only the major pages, css, js, etc, ignore the rest resources, ignore external resources
        // TODO: ignore timeout and get the page source

        return ForwardingResponse(page.url, pageSource, status.minorCode, headers)
    }

    private fun isJsSuccess(pageSource: String): Boolean {
        val p1 = pageSource.indexOf("<body")
        if (p1 <= 0) return false
        val p2 = pageSource.indexOf(">", p1)
        if (p2 <= 0) return false
        // no any link, it's incomplete
        val p3 = pageSource.indexOf("<a", p2)
        if (p3 <= 0) return false

        val bodyTag = pageSource.substring(p1, p2)
        return bodyTag.contains("data-error=\"0\"")
    }

    private fun getResponse(url: String, future: Future<Response>, timeout: Duration): Response {
        // used only for failure
        val status: ProtocolStatus
        val headers = MultiMetadata()

        try {
            // Waits if necessary for at most the given time for the computation
            // to complete, and then retrieves its result, if available.
            return future.get(timeout.seconds, TimeUnit.SECONDS)
        } catch (e: java.util.concurrent.CancellationException) {
            // if the computation was cancelled
            // TODO: this should never happen
            status = ProtocolStatus.failed(ProtocolStatusCodes.CANCELED)
            headers.put("EXCEPTION", e.toString())
        } catch (e: java.util.concurrent.TimeoutException) {
            status = ProtocolStatus.failed(ProtocolStatusCodes.THREAD_TIMEOUT)
            headers.put("EXCEPTION", e.toString())

            log.warn("Fetch resource timeout, $e")
        } catch (e: java.util.concurrent.ExecutionException) {
            status = ProtocolStatus.failed(ProtocolStatusCodes.EXCEPTION)
            headers.put("EXCEPTION", e.toString())

            log.warn("Unexpected exception, {}", StringUtil.stringifyException(e))
        } catch (e: InterruptedException) {
            status = ProtocolStatus.failed(ProtocolStatusCodes.EXCEPTION)
            headers.put("EXCEPTION", e.toString())

            log.warn("Interrupted when fetch resource {}", e)
        } catch (e: Exception) {
            status = ProtocolStatus.failed(ProtocolStatusCodes.EXCEPTION)
            headers.put("EXCEPTION", e.toString())

            log.warn("Unexpected exception, {}", StringUtil.stringifyException(e))
        }

        return ForwardingResponse(url, status.minorCode, headers)
    }

    @Throws(org.openqa.selenium.TimeoutException::class)
    private fun visit(url: String, page: WebPage, driver: WebDriver, driverConfig: DriverConfig) {
        log.debug("Fetching task {}/{} in thread #{}, drivers: {}/{} | {} | timeouts: {}/{}/{}",
                batchTaskCount, totalTaskCount,
                Thread.currentThread().id,
                drivers.freeSize, drivers.totalSize,
                page.configuredUrl,
                driverConfig.pageLoadTimeout, driverConfig.scriptTimeout, driverConfig.scrollDownWait
        )

        val timeouts = driver.manage().timeouts()
        timeouts.pageLoadTimeout(driverConfig.pageLoadTimeout.seconds, TimeUnit.SECONDS)
        timeouts.setScriptTimeout(driverConfig.scriptTimeout.seconds, TimeUnit.SECONDS)
        driver.manage().window().maximize()
        driver.get(url)

        // As a JavascriptExecutor
        if (JavascriptExecutor::class.java.isAssignableFrom(driver.javaClass)) {
            executeJs(url, driver, driverConfig)
        }
    }

    @Throws(org.openqa.selenium.TimeoutException::class)
    private fun executeJs(url: String, driver: WebDriver, driverConfig: DriverConfig) {
        val jsExecutor = driver as? JavascriptExecutor ?: return

        val timeout = driverConfig.pageLoadTimeout.seconds
        val maxRound = timeout - 2
        val wait = FluentWait<WebDriver>(driver)
                .withTimeout(timeout, TimeUnit.SECONDS)
                .pollingEvery(1, TimeUnit.SECONDS)
                // .ignoring(org.openqa.selenium.TimeoutException::class.java)

        // make sure the document is ready
        try {
            val js = ";$libJs;return __utils__.waitForReady($maxRound);"
            val r = wait.until { (it as? JavascriptExecutor)?.executeScript(js) }

            if (r == "timeout") {
                log.debug("Hit max round $maxRound to wait document | {}", url)
            } else {
                log.debug("Document is ready. {} | {}", r, url)
            }
        } catch (e: org.openqa.selenium.TimeoutException) {
            log.info("Timeout to wait document, timeout {}s | {}", timeout, url)
        } catch (e: Exception) {
            // the representation of the underlying DOM
            log.warn("Failed to render document {}, \n{}", url, e)
            throw e
        }

        if (StringUtils.isNotBlank(clientJs)) {
            jsExecutor.executeScript(clientJs)
        }
    }

    private fun getPageSourceSlient(driver: WebDriver): String {
        return try { driver.pageSource } catch (e: Throwable) { "" }
    }

    private fun handleFetchFinish(page: WebPage, driver: WebDriver, headers: MultiMetadata) {
        // The page content's encoding is already converted to UTF-8 by Web driver
        headers.put(CONTENT_ENCODING, "UTF-8")
        headers.put(Q_TRUSTED_CONTENT_ENCODING, "UTF-8")
        headers.put(Q_RESPONSE_TIME, System.currentTimeMillis().toString())
        headers.put(Q_WEB_DRIVER, driver.javaClass.name)

        when (driver) {
            is ChromeDriver -> page.lastBrowser = BrowserType.CHROME
            is HtmlUnitDriver -> page.lastBrowser = BrowserType.HTMLUNIT
            else -> {
                log.warn("Actual browser is set to be NATIVE by selenium engine")
                page.lastBrowser = BrowserType.NATIVE
            }
        }
    }

    private fun handleFetchSuccess(page: WebPage, driver: WebDriver) {
        batchSuccessCount.incrementAndGet()
        totalSuccessCount.incrementAndGet()

        // TODO: A metrics system is required
        log.debug("Selenium task success: {}/{}, total task success: {}/{}",
                batchSuccessCount, batchTaskCount,
                totalSuccessCount, totalTaskCount
        )

        if (totalTaskCount.get() % 100 == 0) {
            log.debug("Total task success: {}/{}", totalSuccessCount, totalTaskCount)
        }
    }

    private fun takeScreenshot(pageSource: String, page: WebPage, driver: RemoteWebDriver, ident: String) {
        if (RemoteWebDriver::class.java.isAssignableFrom(driver.javaClass)) {
            try {
                if (pageSource.length > 100) {
                    val bytes = driver.getScreenshotAs(OutputType.BYTES)
                    export(page, bytes, ident, ".png")
                }
            } catch (e: Exception) {
                log.warn("Cannot take screenshot, page length {} | {}", pageSource.length, page.url)
            }
        }
    }

    private fun handlePageSource(pageSource: String, status: ProtocolStatus, page: WebPage, driver: WebDriver): String {
        if (pageSource.isEmpty()) {
            return ""
        }

        // take the head part and replace charset to UTF-8
        val pos = pageSource.indexOf("</head>")
        var head = pageSource.take(pos)
        // TODO: can still faster
        // Some parsers use html directive to decide the content's encoding, correct it to be UTF-8
        head = charsetPattern.matcher(head).replaceAll("UTF-8")

        // append the rest
        val sb = StringBuilder(head)
        var i = pos
        while (i < pageSource.length) {
            sb.append(pageSource[i])
            ++i
        }

        val content = sb.toString()

        if (log.isDebugEnabled && content.isNotEmpty()) {
            sb.setLength(0)
            sb.append(status.minorName).append('/').append(monthDay)
            if (content.length < 1000) {
                sb.append('/').append(content.length / 200 * 200)
            } else {
                sb.append('/').append(content.length / 2000 * 2000)
            }

            val ident = sb.toString()
            export(page, content.toByteArray(), ident)
            if (log.isTraceEnabled) {
                takeScreenshot(content, page, driver as RemoteWebDriver, ident)
            }
        }

        return content
    }

    private fun handleWebDriverTimeout(url: String, startTime: Long, pageSource: String, driverConfig: DriverConfig) {
        val elapsed = Duration.ofMillis(System.currentTimeMillis() - startTime)
        if (log.isDebugEnabled) {
            log.debug("Selenium timeout. Elapsed {} length {} drivers: {}/{} timeouts: {}/{}/{} | {}",
                    elapsed, pageSource.length,
                    drivers.freeSize, drivers.totalSize,
                    driverConfig.pageLoadTimeout, driverConfig.scriptTimeout, driverConfig.scrollDownWait,
                    url
            )
        } else {
            log.warn("Selenium timeout, elapsed: {} length: {} | {}", elapsed, pageSource.length, url)
        }
    }

    private fun getDriverConfig(priority: Int, page: WebPage, config: ImmutableConfig): DriverConfig {
        // Page load timeout
        val pageLoadTimeout = config.getDuration(FETCH_PAGE_LOAD_TIMEOUT, defaultDriverConfig.pageLoadTimeout)
        // Script timeout
        val scriptTimeout = config.getDuration(FETCH_SCRIPT_TIMEOUT, defaultDriverConfig.scriptTimeout)
        // Scrolling
        var scrollDownCount = config.getInt(FETCH_SCROLL_DOWN_COUNT, defaultDriverConfig.scrollDownCount)
        if (scrollDownCount > 20) {
            scrollDownCount = 20
        }
        var scrollDownWait = config.getDuration(FETCH_SCROLL_DOWN_WAIT, defaultDriverConfig.scrollDownWait)
        if (scrollDownWait > pageLoadTimeout) {
            scrollDownWait = pageLoadTimeout
        }

        return DriverConfig(pageLoadTimeout, scriptTimeout, scrollDownCount, scrollDownWait)
    }

    private fun export(page: WebPage, content: ByteArray, ident: String = "", suffix: String = ".htm") {
        val browser = page.lastBrowser.name.toLowerCase()
        val filename = PulsarPaths.fromUri(page.url, suffix)
        val path = PulsarPaths.get(PulsarPaths.webCacheDir.toString(), browser, ident, filename)
        PulsarFiles.saveTo(content, path, true)
    }

    override fun close() {
        if (isClosed.getAndSet(true)) {
            return
        }

        if (instanceCount.decrementAndGet() <= 0) {
//            executor.close()
//            drivers.close()
        }

        executor.close()
        drivers.close()
    }

    companion object {
        val log = LoggerFactory.getLogger(SeleniumEngine::class.java)!!

        private val batchTaskId = AtomicInteger(0)
        private var instanceCount = AtomicInteger()
        // The javascript to execute by Web browsers
        var browserControl = BrowserControl()
        val defaultSupportedCharsets = "UTF-8|GB2312|GB18030|GBK|Big5|ISO-8859-1" +
                "|windows-1250|windows-1251|windows-1252|windows-1253|windows-1254|windows-1257"
        val systemAvailableCharsets = Charset.availableCharsets().values.joinToString("|") { it.name() }
        // All charsets are supported by the system
        // The set is big, can use a static cache to hold them if necessary
        val defaultCharsetPattern = defaultSupportedCharsets.replace("UTF-8\\|?", "").toPattern(CASE_INSENSITIVE)
        val systemAvailableCharsetPattern = systemAvailableCharsets.replace("UTF-8\\|?", "").toPattern(CASE_INSENSITIVE)
        /**
         * Every process have an unique ImmutableConfig object which is created at the process startup.
         * */
        @Synchronized
        fun getInstance(conf: ImmutableConfig): SeleniumEngine {
            return ObjectCache.get(conf).computeIfAbsent(SeleniumEngine::class.java) { SeleniumEngine(conf) }
        }
    }
}
