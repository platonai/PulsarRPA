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
import ai.platon.pulsar.dom.Documents
import ai.platon.pulsar.persist.ProtocolStatus
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.BrowserType
import ai.platon.pulsar.persist.metadata.MultiMetadata
import ai.platon.pulsar.persist.metadata.Name
import ai.platon.pulsar.persist.metadata.ProtocolStatusCodes
import com.google.common.collect.Iterables
import com.google.common.net.InternetDomainName
import org.apache.commons.codec.digest.DigestUtils
import org.apache.gora.util.TimingUtil
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.remote.UnreachableBrowserException
import org.openqa.selenium.support.ui.FluentWait
import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern.CASE_INSENSITIVE
import kotlin.math.max

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
class SeleniumEngine(
        browserControl: BrowserControl,
        private val executor: GlobalExecutor,
        private val drivers: WebDriverQueues,
        private val immutableConfig: ImmutableConfig
): Parameterized, AutoCloseable {
    val log = LoggerFactory.getLogger(SeleniumEngine::class.java)!!

    private val libJs = browserControl.parseLibJs(false)
    private val clientJs = browserControl.parseJs(false)

    private val supportAllCharsets get() = immutableConfig.getBoolean(PARSE_SUPPORT_ALL_CHARSETS, false)
    private var charsetPattern = if (supportAllCharsets) systemAvailableCharsetPattern else defaultCharsetPattern
    private val defaultDriverConfig = DriverConfig(immutableConfig)

    private val monthDay = DateTimeUtil.now("MMdd")

    private val isClosed = AtomicBoolean(false)

    init {
        instanceCount.incrementAndGet()
        params.withLogger(log).info()
    }

    override fun getParams(): Params {
        return Params.of(
                "instanceCount", instanceCount,
                "charsetPattern", charsetPattern,
                "pageLoadTimeout", defaultDriverConfig.pageLoadTimeout,
                "scriptTimeout", defaultDriverConfig.scriptTimeout,
                "scrollDownCount", defaultDriverConfig.scrollDownCount,
                "scrollDownWait", defaultDriverConfig.scrollDownWait,
                "clientJsLength", clientJs.length,
                "maxWebDrivers", drivers.capacity
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
        return fetchContentInternal(nextBatchId, priority, page, conf)
    }

    fun fetchAll(batchId: Int, urls: Iterable<String>): Collection<Response> {
        return urls.map { this.fetch(it) }
    }

    fun fetchAll(urls: Iterable<String>): Collection<Response> {
        return fetchAll(nextBatchId, urls)
    }

    fun fetchAll(batchId: Int, urls: Iterable<String>, mutableConfig: MutableConfig): Collection<Response> {
        return urls.map { fetch(it, mutableConfig) }
    }

    fun fetchAll(urls: Iterable<String>, mutableConfig: MutableConfig): Collection<Response> {
        return fetchAll(nextBatchId, urls, mutableConfig)
    }

    fun parallelFetchAll(urls: Iterable<String>, mutableConfig: MutableConfig): Collection<Response> {
        return parallelFetchAllPages(urls.map { WebPage.newWebPage(it) }, mutableConfig)
    }

    fun parallelFetchAll(batchId: Int, urls: Iterable<String>, mutableConfig: MutableConfig): Collection<Response> {
        return parallelFetchAllPages(batchId, urls.map { WebPage.newWebPage(it) }, mutableConfig)
    }

    fun parallelFetchAllPages(pages: Iterable<WebPage>, mutableConfig: MutableConfig): Collection<Response> {
        return parallelFetchAllPages(nextBatchId, pages, mutableConfig)
    }

    fun parallelFetchAllPages(batchId: Int, pages: Iterable<WebPage>, mutableConfig: MutableConfig): Collection<Response> {
        val startTime = Instant.now()
        val size = Iterables.size(pages)

        log.info("Start batch task {} with {} pages in parallel", batchId, size)

        val priority = mutableConfig.getUint(SELENIUM_WEB_DRIVER_PRIORITY, 0)!!

        // Create a task submitter
        val submitter = { i: Int, page: WebPage ->
            executor.submit { fetchContentInternal(batchId, i, priority, page, mutableConfig) }
        }

        // Submit all tasks
        var i = 0
        val pendingTasks = pages.associateTo(HashMap()) { it.url to submitter(++i, it) }
        val finishedTasks = mutableMapOf<String, Response>()

        // The function must return in a reasonable time
        val threadTimeout = mutableConfig.getDuration(FETCH_PAGE_LOAD_TIMEOUT).plusSeconds(10)
        val numTotalTasks = pendingTasks.size
        val interval = Duration.ofSeconds(1)
        val idleTimeout = Duration.ofMinutes(2).seconds
        val numAllowedFailures = max(10, numTotalTasks / 3)

        var numFinishedTasks = 0
        var numFailedTasks = 0
        var idleSeconds = 0
        var bytes = 0

        // since the urls in the batch are usually from the same domain,
        // if there are too many failure, the rest tasks are very likely run to failure too
        i = 0
        while (numFinishedTasks < numTotalTasks && numFailedTasks <= numAllowedFailures && idleSeconds < idleTimeout
                && !isClosed.get() && !Thread.currentThread().isInterrupted) {
            ++i

            if (i >= 60 && i % 30 == 0) {
                // report every 30 round if it takes long time
                log.warn("Batch {} takes long time - round {} - {} pending, {} finished, {} failed, idle: {}s, idle timeout: {}s",
                        batchId, i, pendingTasks.size, finishedTasks.size, numFailedTasks, idleSeconds, idleTimeout)
            }

            // loop and wait for all parallel tasks return
            var numTaskDone = 0
            val removal = mutableListOf<String>()
            pendingTasks.asSequence().filter { it.value.isDone }.forEach { (key, future) ->
                if (isClosed.get() || Thread.currentThread().isInterrupted) {
                    return@forEach
                }

                try {
                    val response = getResponse(key, future, threadTimeout)
                    bytes += response.size()
                    val time = Duration.ofSeconds(i.toLong())// response.headers
                    val isFailed = response.code !in arrayOf(ProtocolStatusCodes.SUCCESS_OK, ProtocolStatusCodes.CANCELED)
                    if (isFailed) {
                        ++numFailedTasks
                        if (log.isInfoEnabled) {
                            log.info("Batch {} round {} task failed, reason {}, {} bytes in {}, total {} failed | {}",
                                    batchId, String.format("%2d", i),
                                    ProtocolStatus.getMinorName(response.code), String.format("%,d", response.size()), time,
                                    numFailedTasks,
                                    key
                            )
                        }
                    } else {
                        if (log.isDebugEnabled) {
                            // TODO: track timeout
                            log.debug("Batch {} round {} fetched{}{} bytes in {} with code {} | {}",
                                    batchId, String.format("%2d", i),
                                    if (bytes < 2000) " only " else " ", String.format("%,7d", response.size()),
                                    time,
                                    response.code, key
                            )
                        }
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

            if (numFinishedTasks > 5) {
                // We may do something eagerly
            }

            idleSeconds = if (numTaskDone == 0) 1 + idleSeconds else 0

            try {
                TimeUnit.SECONDS.sleep(interval.seconds)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                log.warn("Selenium interrupted, {} pending tasks will be canceled", pendingTasks.size)
            }
        }

        if (pendingTasks.isNotEmpty()) {
            log.warn("Batch task is incomplete, finished tasks {}, pending tasks {}, failed tasks: {}, idle: {}s",
                    finishedTasks.size, pendingTasks.size, numFailedTasks, idleSeconds)

            // if there are still pending tasks, cancel them
            pendingTasks.forEach { it.value.cancel(true) }
            pendingTasks.forEach { (url, task) ->
                // Attempts to cancel execution of this task
                try {
                    val response = getResponse(url, task, threadTimeout)
                    finishedTasks[url] = response
                } catch (e: Throwable) {
                    log.error("Unexpected error {}", StringUtil.stringifyException(e))
                }
            }
        }

        val elapsed = Duration.between(startTime, Instant.now())
        log.info("Batch {} with {} tasks is finished in {}, ave time {}s, ave bytes: {}, speed: {}bps",
                batchId, size, elapsed,
                String.format("%,.2f", 1.0 * elapsed.seconds / size),
                bytes / size,
                String.format("%,.2f", 1.0 * bytes * 8 / elapsed.seconds)
        )

        return finishedTasks.values
    }

    private fun fetchContentInternal(page: WebPage): Response {
        val config = page.mutableConfig ?: immutableConfig
        val priority = config.getUint(SELENIUM_WEB_DRIVER_PRIORITY, 0)
        return fetchContentInternal(nextBatchId, priority, page, config)
    }

    private fun fetchContentInternal(batchId: Int, priority: Int, page: WebPage, config: ImmutableConfig): Response {
        return fetchContentInternal(batchId, 0, priority, page, config)
    }

    private fun fetchContentInternal(batchId: Int, taskId: Int, priority: Int, page: WebPage, config: ImmutableConfig): Response {
        if (isClosed.get()) {
            return ForwardingResponse(page.url, "", ProtocolStatus.STATUS_CANCELED, MultiMetadata())
        }

        val driver = drivers.poll(priority, config)
                ?: return ForwardingResponse(page.url, "", ProtocolStatus.STATUS_RETRY, MultiMetadata())
        totalTaskCount.getAndIncrement()
        batchTaskCounters.computeIfAbsent(batchId) { AtomicInteger() }.incrementAndGet()

        try {
            return fetchContentInternal1(driver, batchId, taskId, priority, page, config)
        } finally {
            drivers.put(priority, driver)
        }
    }

    /**
     * Must be thread safe
     */
    private fun fetchContentInternal1(
            driver: WebDriver,
            batchId: Int,
            taskId: Int,
            priority: Int,
            page: WebPage,
            config: ImmutableConfig
    ): Response {
        // page.baseUrl is the last working address, and page.url is the permanent internal address
        var location = page.location
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
            status = visit(batchId, taskId, location, page, driver, driverConfig)
            pageSource = getPageSourceSlient(driver)
            // TODO: handle with frames
            // driver.switchTo().frame(1);
        } catch (e: TimeoutException) {
            // log.warn(e.toString())
            pageSource = getPageSourceSlient(driver)
            status = ProtocolStatus.failed(ProtocolStatusCodes.WEB_DRIVER_TIMEOUT)
        } catch (e: org.openqa.selenium.NoSuchElementException) {
            // failed to wait for body
            status = ProtocolStatus.STATUS_RETRY
            log.warn(e.toString())
        } catch (e: WebDriverException) {
            status = ProtocolStatus.STATUS_RETRY
            log.warn(e.toString())
        } catch (e: Throwable) {
            // must not throw again
            status = ProtocolStatus.STATUS_EXCEPTION
            log.warn("Unexpected exception: {}", e)
        }

        if (status.minorCode == ProtocolStatusCodes.WEB_DRIVER_TIMEOUT
                || status.minorCode == ProtocolStatusCodes.DOCUMENT_READY_TIMEOUT) {
            // The javascript set data-error flag to indicate if the vision information of all DOM nodes are calculated
            if (isJsSuccess(pageSource)) {
                status = ProtocolStatus.STATUS_SUCCESS
            }

            if (status.isSuccess) {
                log.info("Document ok but timeout after {} with {} bytes | {}",
                        DateTimeUtil.elapsedTime(startTime),
                        String.format("%,7d", pageSource.length), page.url)
            } else {
                handleWebDriverTimeout(page.url, startTime, pageSource, driverConfig)
            }
        }

        handleFetchFinish(page, driver, headers)
        pageSource = handlePageSource(pageSource, status, page, driver)
        headers.put(CONTENT_LENGTH, pageSource.length.toString())
        if (status.isSuccess) {
            handleFetchSuccess(batchId)
        }

        // TODO: handle redirect
        // TODO: collect response header
        // TODO: fetch only the major pages, css, js, etc, ignore the rest resources, ignore external resources
        // TODO: ignore timeout and get the page source

        return ForwardingResponse(page.url, pageSource, status, headers)
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
            status = ProtocolStatus.failed(ProtocolStatusCodes.RETRY)
            headers.put("EXCEPTION", e.toString())

            log.warn("Unexpected exception, {}", StringUtil.stringifyException(e))
        } catch (e: InterruptedException) {
            status = ProtocolStatus.failed(ProtocolStatusCodes.RETRY)
            headers.put("EXCEPTION", e.toString())

            log.warn("Interrupted when fetch resource {}", e)
        } catch (e: Exception) {
            status = ProtocolStatus.failed(ProtocolStatusCodes.EXCEPTION)
            headers.put("EXCEPTION", e.toString())

            log.warn("Unexpected exception, {}", StringUtil.stringifyException(e))
        }

        return ForwardingResponse(url, "", status, headers)
    }

    private fun visit(
            batchId: Int,
            taskId: Int,
            url: String,
            page: WebPage,
            driver: WebDriver,
            driverConfig: DriverConfig
    ): ProtocolStatus {
        log.info("Fetching task {}/{}/{} in thread {}, drivers: {}/{} | {} | timeouts: {}/{}/{}",
                taskId, batchTaskCounters[batchId], totalTaskCount,
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

        var status = ProtocolStatus.STATUS_SUCCESS

        // Block and wait for the document is ready: all css and resources are OK
        if (JavascriptExecutor::class.java.isAssignableFrom(driver.javaClass)) {
            status = executeJs(url, driver, driverConfig)
        }

        return status
    }

    @Throws(org.openqa.selenium.TimeoutException::class)
    private fun executeJs(url: String, driver: WebDriver, driverConfig: DriverConfig): ProtocolStatus {
        val jsExecutor = driver as? JavascriptExecutor?: return ProtocolStatus.STATUS_RETRY

        var status = ProtocolStatus.STATUS_SUCCESS
        val timeout = driverConfig.pageLoadTimeout.seconds
        val scroll = driverConfig.scrollDownCount
        val maxRound = timeout - 2
        val wait = FluentWait<WebDriver>(driver)
                .withTimeout(timeout, TimeUnit.SECONDS)
                .pollingEvery(1, TimeUnit.SECONDS)
                .ignoring(InterruptedException::class.java)
                // .ignoring(org.openqa.selenium.TimeoutException::class.java)

        // make sure the document is ready
        try {
            val js = ";$libJs;return __utils__.waitForReady($maxRound, $scroll);"
            val r = wait.until { (it as? JavascriptExecutor)?.executeScript(js) }

            if (r == "timeout") {
                log.debug("Hit max round $maxRound to wait document | {}", url)
            } else {
                log.trace("Document is ready. {} | {}", r, url)
            }
        } catch (e: org.openqa.selenium.TimeoutException) {
            log.trace("Timeout to wait for document ready, timeout {}s | {}", timeout, url)
            status = ProtocolStatus.failed(ProtocolStatusCodes.DOCUMENT_READY_TIMEOUT)
        } catch (e: InterruptedException) {
            log.warn("Waiting for document interrupted | {}", url)
            Thread.currentThread().interrupt()
            status = ProtocolStatus.failed(ProtocolStatusCodes.CANCELED)
        } catch (e: UnreachableBrowserException) {
            log.warn("Browser unreachable | {}", url)
            status = ProtocolStatus.failed(ProtocolStatusCodes.CANCELED)
        } catch (e: WebDriverException) {
            if (e.cause is org.apache.http.conn.HttpHostConnectException) {
                // Web driver closed
            } else if (e.cause is InterruptedException) {
                // Web driver closed
            } else {
                log.warn("Web driver exception | {} \n>>>\n{}\n<<<", url, e.message)
            }
            status = ProtocolStatus.failed(ProtocolStatusCodes.CANCELED)
        } catch (e: Exception) {
            log.warn("Unexpected exception | {}", url)
            log.warn(StringUtil.stringifyException(e))
            throw e
        }

        if (status.isSuccess) {
            jsExecutor.executeScript(clientJs)
        }

        return status
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

    private fun handleFetchSuccess(batchId: Int) {
        batchSuccessCounters[batchId]?.incrementAndGet()
        totalSuccessCount.incrementAndGet()

        // TODO: A metrics system is required
        if (totalTaskCount.get() % 20 == 0) {
            log.debug("Selenium task success: {}/{}, total task success: {}/{}",
                    batchSuccessCounters[batchId], batchTaskCounters[batchId], totalSuccessCount, totalTaskCount)
        }
    }

    private fun takeScreenshot(contentLength: Int, page: WebPage, driver: RemoteWebDriver) {
        if (RemoteWebDriver::class.java.isAssignableFrom(driver.javaClass)) {
            try {
                if (contentLength > 100) {
                    val bytes = driver.getScreenshotAs(OutputType.BYTES)
                    export(page, bytes, ".png")
                }
            } catch (e: Exception) {
                log.warn("Cannot take screenshot, page length {} | {}", contentLength, page.url)
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
            export(sb, status, content, page)

            if (log.isTraceEnabled) {
                takeScreenshot(content.length, page, driver as RemoteWebDriver)
            }
        }

        return content
    }

    private fun export(sb: StringBuilder, status: ProtocolStatus, content: String, page: WebPage) {
        val document = Documents.parse(content, page.baseUrl)
        document.absoluteLinks()
        val prettyHtml = document.prettyHtml

        sb.setLength(0)
        sb.append(status.minorName).append('/').append(monthDay)
        if (prettyHtml.length < 2000) {
            sb.append("/a").append(prettyHtml.length / 500 * 500)
        } else {
            sb.append("/b").append(prettyHtml.length / 20000 * 20000)
        }

        val ident = sb.toString()
        val path = export(page, prettyHtml.toByteArray(), ident)

        page.metadata.set(Name.ORIGINAL_EXPORT_PATH, path.toString())
    }

    private fun handleWebDriverTimeout(url: String, startTime: Long, pageSource: String, driverConfig: DriverConfig) {
        val elapsed = Duration.ofMillis(System.currentTimeMillis() - startTime)
        if (log.isDebugEnabled) {
            log.debug("Selenium timeout,  elapsed {} length {} drivers: {}/{} timeouts: {}/{}/{} | {}",
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

    private fun export(page: WebPage, content: ByteArray, ident: String = "", suffix: String = ".htm"): Path {
        val browser = page.lastBrowser.name.toLowerCase()
        val u = Urls.getURLOrNull(page.url)?: return PulsarPaths.tmpDir
        val domain = InternetDomainName.from(u.host).topPrivateDomain().toString()
        val filename = ident + "-" + DigestUtils.md5Hex(page.url) + suffix
        val path = PulsarPaths.get(PulsarPaths.webCacheDir.toString(), "original", browser, domain, filename)
        PulsarFiles.saveTo(content, path, true)
        return path
    }

    override fun close() {
        if (isClosed.getAndSet(true)) {
            return
        }
    }

    companion object {
        private var instanceCount = AtomicInteger()
        val defaultSupportedCharsets = "UTF-8|GB2312|GB18030|GBK|Big5|ISO-8859-1" +
                "|windows-1250|windows-1251|windows-1252|windows-1253|windows-1254|windows-1257"
        val systemAvailableCharsets = Charset.availableCharsets().values.joinToString("|") { it.name() }
        // All charsets are supported by the system
        // The set is big, can use a static cache to hold them if necessary
        val defaultCharsetPattern = defaultSupportedCharsets.replace("UTF-8\\|?", "").toPattern(CASE_INSENSITIVE)
        val systemAvailableCharsetPattern = systemAvailableCharsets.replace("UTF-8\\|?", "").toPattern(CASE_INSENSITIVE)

        private val batchIdGen = AtomicInteger(0)
        val nextBatchId get() = batchIdGen.incrementAndGet()

        val totalTaskCount = AtomicInteger(0)
        val totalSuccessCount = AtomicInteger(0)

        val batchTaskCounters = Collections.synchronizedMap(mutableMapOf<Int, AtomicInteger>())
        val batchSuccessCounters = Collections.synchronizedMap(mutableMapOf<Int, AtomicInteger>())
    }
}
