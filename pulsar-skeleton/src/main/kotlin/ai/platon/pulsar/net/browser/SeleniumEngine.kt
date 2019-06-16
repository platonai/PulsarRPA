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
import org.apache.commons.lang3.StringUtils
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.OutputType
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.support.ui.FluentWait
import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import java.time.Duration
import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern.CASE_INSENSITIVE

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
 */
class SeleniumEngine(val immutableConfig: ImmutableConfig): Parameterized, AutoCloseable {

    private val executor: GlobalExecutor = GlobalExecutor.getInstance(immutableConfig)
    private val drivers: WebDriverQueues = WebDriverQueues(browserControl, immutableConfig)

    private val supportAllCharsets get() = immutableConfig.getBoolean(PARSE_SUPPORT_ALL_CHARSETS, false)
    private var charsetPattern = if (supportAllCharsets) systemAvailableCharsetPattern else defaultCharsetPattern
    private val defaultDriverConfig = DriverConfig(immutableConfig)
    private var clientJs = browserControl.parseJs(true)

    private val totalTaskCount = AtomicInteger(0)
    private val totalSuccessCount = AtomicInteger(0)

    // TODO: associate by batch id
    private val batchTaskCount = AtomicInteger(0)
    private val batchSuccessCount = AtomicInteger(0)

    private val closed = AtomicBoolean(false)

    init {
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

        // create a task submitter
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
        headers.put(Q_REQUEST_TIME, System.currentTimeMillis().toString())

        try {
            visit(location, page, driver, driverConfig)
            status = ProtocolStatus.STATUS_SUCCESS
            // TODO: handle with frames
            // driver.switchTo().frame(1);
        } catch (e: org.openqa.selenium.TimeoutException) {
            log.warn(e.toString())
            handleWebDriverTimeout(page.url, driver, driverConfig)
            // TODO: the reason may be one of page load timeout, script timeout and implicit wait timeout
            status = ProtocolStatus.failed(ProtocolStatusCodes.WEB_DRIVER_TIMEOUT)
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
            log.warn("Unexpected exception: {}", e.toString())
        }

        var pageSource = ""

        try {
            pageSource = handlePageSource(status, page, driver)
            handleFetchFinish(pageSource, page, driver, headers)
            if (status.isSuccess) {
                handleFetchSuccess(pageSource, page, driver)
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

            log.warn("url: {}, {}", url, e)
        }

        return ForwardingResponse(url, status.minorCode, headers)
    }

    @Throws(org.openqa.selenium.TimeoutException::class)
    private fun visit(url: String, page: WebPage, driver: WebDriver, driverConfig: DriverConfig) {
        log.debug("Fetching task: {}/{}, thread: #{}, drivers: {}/{}, timeouts: {}/{}/{}, {}",
                batchTaskCount, totalTaskCount,
                Thread.currentThread().id,
                drivers.freeSize, drivers.totalSize,
                driverConfig.pageLoadTimeout, driverConfig.scriptTimeout, driverConfig.scrollDownWait,
                page.configuredUrl
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

        val wait = FluentWait<WebDriver>(driver)
                .withTimeout(driverConfig.pageLoadTimeout.seconds, TimeUnit.SECONDS)
                .pollingEvery(500, TimeUnit.MILLISECONDS)

        // make sure the document is ready
        try {
            wait.until { dr -> dr.findElement(By.tagName("body")) }
        } catch (e: Exception) {
            // the representation of the underlying DOM
            log.warn("Failed to render document {}, length: {}", url, driver.pageSource.length)
            throw e
        }

        // Scroll down times to ensure ajax content can be loaded
        // TODO: no way to determine if there is ajax response while it scrolls, i remember phantomjs has a way
        for (i in 1..driverConfig.scrollDownCount) {
            log.trace("Scrolling down #{}", i)

            // TODO: some websites do not allow scroll to below the bottom
            val scrollJs = ";window.scrollBy(0, 500);"
            jsExecutor.executeScript(scrollJs)

            // TODO: is there better way to do this?
            val millis = driverConfig.scrollDownWait.toMillis()
            if (millis > 0) {
                try { TimeUnit.MILLISECONDS.sleep(millis) } catch (e: InterruptedException) {
                    log.warn("Scrolling interrupted. {}", e)
                    break
                }
            }
        }

        // now we must stop everything
        if (driverConfig.scrollDownCount > 0) {
            jsExecutor.executeScript(";return window.stop();")
        }

        if (StringUtils.isNotBlank(clientJs)) {
            jsExecutor.executeScript(clientJs)
        }

        jsExecutor.executeScript(";return window.stop();")
    }

    private fun handleFetchFinish(pageSource: String, page: WebPage, driver: WebDriver, headers: MultiMetadata) {
        // The page content's encoding is already converted to UTF-8 by Web driver
        headers.put(CONTENT_ENCODING, "UTF-8")
        headers.put(CONTENT_LENGTH, pageSource.length.toString())
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

    private fun handleFetchSuccess(pageSource: String, page: WebPage, driver: WebDriver) {
        batchSuccessCount.incrementAndGet()
        totalSuccessCount.incrementAndGet()

        // TODO: A metrics system is required
        log.debug("Selenium batch task success: {}/{}, total task success: {}/{}",
                batchSuccessCount, batchTaskCount,
                totalSuccessCount, totalTaskCount
        )

        if (totalTaskCount.get() % 100 == 0) {
            log.debug("Total task success: {}/{}", totalSuccessCount, totalTaskCount)
        }
        // headers.put(CONTENT_TYPE, "");
    }

    private fun takeScreenshot(page: WebPage, driver: RemoteWebDriver, ident: String) {
        if (RemoteWebDriver::class.java.isAssignableFrom(driver.javaClass)) {
            var length = 0
            try {
                val bytes = driver.getScreenshotAs(OutputType.BYTES)
                length = bytes.size
                export(page, bytes, ident, ".png")
            } catch (e: Exception) {
                log.warn("Failed to take screenshot, url: {} | bytes: {}", page.url, length)
                log.warn(e.toString())
            }
        }
    }

    private fun handlePageSource(status: ProtocolStatus, page: WebPage, driver: WebDriver): String {
        val pageSource = driver.pageSource

        if (pageSource.isEmpty()) {
            return ""
        }

        // Some parsers use html directive to decide the content's encoding, correct it to be UTF-8
        // TODO: Do it only for html content
        // TODO: Replace only corresponding html meta directive, not all occurrence
        val content = charsetPattern.matcher(pageSource).replaceFirst("UTF-8")

        if (log.isDebugEnabled && content.isNotEmpty()) {
            val ident = status.minorName
            export(page, content.toByteArray(), ident)
            takeScreenshot(page, driver as RemoteWebDriver, ident)
        }

        return content
    }

    private fun handleWebDriverTimeout(url: String, driver: WebDriver, driverConfig: DriverConfig) {
        val pageSource = driver.pageSource
        if (log.isDebugEnabled) {
            log.debug("Selenium timeout. Timeouts: {}/{}/{}, drivers: {}/{}, length: {}, url: {}",
                    driverConfig.pageLoadTimeout, driverConfig.scriptTimeout, driverConfig.scrollDownWait,
                    drivers.freeSize, drivers.totalSize,
                    pageSource.length, url
            )
        } else {
            log.warn("Selenium timeout, length: {}, url: {}", pageSource.length, url)
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
        if (closed.getAndSet(true)) {
            return
        }

        executor.close()
        drivers.close()
    }

    companion object {
        val log = LoggerFactory.getLogger(SeleniumEngine::class.java)!!

        // The javascript to execute by Web browsers
        var browserControl = BrowserControl()
        private val batchTaskId = AtomicInteger(0)
        val defaultSupportedCharsets = "UTF-8|GB2312|GB18030|GBK|Big5|ISO-8859-1" +
                "|windows-1250|windows-1251|windows-1252|windows-1253|windows-1254|windows-1257"
        val systemAvailableCharsets = Charset.availableCharsets().values.joinToString("|") { it.name() }
        // All charsets are supported by the system
        // The set is big, can use a static cache to hold them if necessary
        val defaultCharsetPattern = defaultSupportedCharsets.replace("UTF-8\\|?", "").toPattern(CASE_INSENSITIVE)
        val systemAvailableCharsetPattern = systemAvailableCharsets.replace("UTF-8\\|?", "").toPattern(CASE_INSENSITIVE)

        fun getInstance(conf: ImmutableConfig): SeleniumEngine {
            return ObjectCache.get(conf).computeIfAbsent(SeleniumEngine::class.java) { SeleniumEngine(conf) }
        }
    }
}
