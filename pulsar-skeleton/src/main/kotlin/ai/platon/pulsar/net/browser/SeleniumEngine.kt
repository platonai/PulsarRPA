package ai.platon.pulsar.net.browser

import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.HttpHeaders.*
import ai.platon.pulsar.common.config.CapabilityTypes.*
import ai.platon.pulsar.common.config.ImmutableConfig
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
import com.google.common.net.InternetDomainName
import org.apache.commons.codec.digest.DigestUtils
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
import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern.CASE_INSENSITIVE

data class PulsarJsStatus(
        val n: Int = 0,
        val scroll: Int = 0,
        val st: String = "",
        val r: String = "",
        val idl: String = ""
)

data class PulsarJsStat(
        val ni: Int = 0,
        val na: Int = 0,
        val nnm: Int = 0,
        val nst: Int = 0,
        val w: Int = 0,
        val h: Int = 0
)

data class PulsarJsData(
        val status: PulsarJsStatus = PulsarJsStatus(),
        val initStat: PulsarJsStat = PulsarJsStat(),
        val lastStat: PulsarJsStat = PulsarJsStat(),
        val initD: PulsarJsStat = PulsarJsStat(),
        val lastD: PulsarJsStat = PulsarJsStat()
)

data class DriverConfig(
        var pageLoadTimeout: Duration,
        var scriptTimeout: Duration,
        var scrollDownCount: Int,
        var scrollInterval: Duration
) {
    constructor(config: ImmutableConfig): this(
            config.getDuration(FETCH_PAGE_LOAD_TIMEOUT, Duration.ofSeconds(60)),
            // wait page ready using script, so it can not smaller than pageLoadTimeout
            config.getDuration(FETCH_SCRIPT_TIMEOUT, Duration.ofSeconds(60)),
            config.getInt(FETCH_SCROLL_DOWN_COUNT, 5),
            config.getDuration(FETCH_SCROLL_DOWN_INTERVAL, Duration.ofMillis(500))
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
                "scrollInterval", defaultDriverConfig.scrollInterval,
                "clientJsLength", clientJs.length,
                "maxWebDrivers", drivers.capacity
        )
    }

    internal fun fetchContentInternal(batchId: Int, priority: Int, page: WebPage, config: ImmutableConfig): Response {
        return fetchContentInternal(batchId, 0, priority, page, config)
    }

    internal fun fetchContentInternal(batchId: Int, taskId: Int, priority: Int, page: WebPage, config: ImmutableConfig): Response {
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
            // TODO: handle with frames
            // driver.switchTo().frame(1);
        } catch (e: TimeoutException) {
            // log.warn(e.toString())
            status = ProtocolStatus.failed(ProtocolStatusCodes.WEB_DRIVER_TIMEOUT)
        } catch (e: org.openqa.selenium.NoSuchElementException) {
            // failed to wait for body
            status = ProtocolStatus.STATUS_RETRY
            log.warn(e.toString())
        } catch (e: org.openqa.selenium.WebDriverException) {
            status = ProtocolStatus.STATUS_RETRY
            log.warn(e.toString())
        } catch (e: Throwable) {
            // must not throw again
            status = ProtocolStatus.STATUS_EXCEPTION
            log.warn("Unexpected exception: {}", e)
        }

        pageSource = getPageSourceSilently(driver)
        if (status.minorCode == ProtocolStatusCodes.WEB_DRIVER_TIMEOUT
                || status.minorCode == ProtocolStatusCodes.DOCUMENT_READY_TIMEOUT) {
            // The javascript set data-error flag to indicate if the vision information of all DOM nodes are calculated
            val oldStatus = status
            val integrity = checkHtmlIntegrity(pageSource)
            if (integrity.first) {
                status = ProtocolStatus.STATUS_SUCCESS
            }

            if (status.isSuccess) {
                log.info("Html is OK but timeout ({}) after {} with {} bytes | {}",
                        oldStatus.minorName, DateTimeUtil.elapsedTime(startTime),
                        String.format("%,7d", pageSource.length), page.url)
            } else {
                log.info("Timeout with page source check {}", integrity.second)
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

    private fun checkHtmlIntegrity(pageSource: String): Pair<Boolean, String> {
        val p1 = pageSource.indexOf("<body")
        if (p1 <= 0) return false to "NO_BODY_START"
        val p2 = pageSource.indexOf(">", p1)
        if (p2 < p1) return false to "NO_BODY_END"
        // no any link, it's incomplete
        val p3 = pageSource.indexOf("<a", p2)
        if (p3 < p2) return false to "NO_ANCHOR"

        // TODO: optimization using region match
        val bodyTag = pageSource.substring(p1, p2)
        val r = bodyTag.contains("data-error=\"0\"")
        if (!r) {
            return false to "NO_JS_OK"
        }

        return true to "OK"
    }

    fun getResponse(url: String, future: Future<Response>, timeout: Duration): Response {
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

    private fun visit(batchId: Int, taskId: Int, url: String, page: WebPage,
            driver: WebDriver, driverConfig: DriverConfig): ProtocolStatus {
        log.info("Fetching task {}/{}/{} in thread {}, drivers: {}/{} | {} | timeouts: {}/{}/{}",
                taskId, batchTaskCounters[batchId], totalTaskCount,
                Thread.currentThread().id,
                drivers.freeSize, drivers.totalSize,
                page.configuredUrl,
                driverConfig.pageLoadTimeout, driverConfig.scriptTimeout, driverConfig.scrollInterval
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

    private fun executeJs(url: String, driver: WebDriver, driverConfig: DriverConfig): ProtocolStatus {
        val jsExecutor = driver as? JavascriptExecutor?: return ProtocolStatus.STATUS_CANCELED

        var status = ProtocolStatus.STATUS_SUCCESS
        val pageLoadTimeout = driverConfig.pageLoadTimeout.seconds

        try {
            val documentWait = FluentWait<WebDriver>(driver)
                    .withTimeout(pageLoadTimeout, TimeUnit.SECONDS)
                    .pollingEvery(1, TimeUnit.SECONDS)
                    .ignoring(InterruptedException::class.java)

            try {
                // make sure the document is ready
                val initialScroll = 2
                val maxRound = pageLoadTimeout - 10 //leave 10 seconds to wait for script finish
                val js = ";$libJs;return __utils__.waitForReady($maxRound, $initialScroll);"
                val r = documentWait.until { (it as? JavascriptExecutor)?.executeScript(js) }

                if (r == "timeout") {
                    log.debug("Hit max round $maxRound to wait document | {}", url)
                } else {
                    log.trace("Document is ready. {} | {}", r, url)
                }
            } catch (e: org.openqa.selenium.TimeoutException) {
                log.trace("Timeout to wait for document ready, timeout {}s | {}", pageLoadTimeout, url)
                status = ProtocolStatus.failed(ProtocolStatusCodes.DOCUMENT_READY_TIMEOUT)
            }

            performScrollDown(driver, driverConfig)
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

        val data = jsExecutor.executeScript(clientJs)
        if (log.isDebugEnabled) {
            if (data is MutableMap<*, *>) {
                log.debug("{} | {}", formatPulsarData(data), url)
            }
        }

        return status
    }

    private fun formatPulsarData(data: MutableMap<*, *>): String {
        val s1 = data["initStat"] as? Map<*, *>
        val s2 = data["lastStat"] as? Map<*, *>
        val s3 = data["initD"] as? Map<*, *>
        val s4 = data["lastD"] as? Map<*, *>
        val s = String.format(
                "img: %s/%s/%s/%s, a: %s/%s/%s/%s, num: %s/%s/%s/%s, st: %s/%s/%s/%s, " +
                        "w: %s/%s/%s/%s, h: %s/%s/%s/%s",
                s1?.get("ni"),  s2?.get("ni"),  s3?.get("ni"),  s4?.get("ni"),
                s1?.get("na"),  s2?.get("na"),  s3?.get("na"),  s4?.get("na"),
                s1?.get("nnm"), s2?.get("nnm"), s3?.get("nnm"), s4?.get("nnm"),
                s1?.get("nst"), s2?.get("nst"), s3?.get("nst"), s4?.get("nst"),
                s1?.get("w"),   s2?.get("w"),   s3?.get("w"),   s4?.get("w"),
                s1?.get("h"),   s2?.get("h"),   s3?.get("h"),   s4?.get("h")
        )
        val st = data["status"] as? Map<*, *>
        val m = String.format("n:%s scroll:%s st:%s r:%s idl:%s\t%s\t(is,ls,id,ld)",
                st?.get("n"), st?.get("scroll"), st?.get("st"), st?.get("r"), st?.get("idl"), s)
        return m
    }

    private fun performScrollDown(driver: WebDriver, driverConfig: DriverConfig) {
        val scrollDownCount = driverConfig.scrollDownCount.toLong()
        val scrollDownWait = driverConfig.scrollInterval
        val timeout = scrollDownCount * scrollDownWait.toMillis() + 3 * 1000
        val scrollWait = FluentWait<WebDriver>(driver)
                .withTimeout(timeout, TimeUnit.MILLISECONDS)
                .pollingEvery(scrollDownWait.toMillis(), TimeUnit.MILLISECONDS)
                .ignoring(org.openqa.selenium.TimeoutException::class.java)

        try {
            // TODO: which one is the better? browser side timer or selenium side timer?
            val js = ";$libJs;return __utils__.scrollDownN($scrollDownCount);"
            scrollWait.until { (it as? JavascriptExecutor)?.executeScript(js) }
        } catch (e: org.openqa.selenium.TimeoutException) {
            // ignore
        }
    }

    private fun getPageSourceSilently(driver: WebDriver): String {
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
                    elapsed, String.format("%,7d", pageSource.length),
                    drivers.freeSize, drivers.totalSize,
                    driverConfig.pageLoadTimeout, driverConfig.scriptTimeout, driverConfig.scrollInterval,
                    url
            )
        } else {
            log.warn("Selenium timeout, elapsed: {} length: {} | {}", elapsed, String.format("%,7d", pageSource.length), url)
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
        var scrollDownWait = config.getDuration(FETCH_SCROLL_DOWN_INTERVAL, defaultDriverConfig.scrollInterval)
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

        val totalTaskCount = AtomicInteger(0)
        val totalSuccessCount = AtomicInteger(0)

        val batchTaskCounters = Collections.synchronizedMap(mutableMapOf<Int, AtomicInteger>())
        val batchSuccessCounters = Collections.synchronizedMap(mutableMapOf<Int, AtomicInteger>())
    }
}
