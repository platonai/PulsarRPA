package ai.platon.pulsar.protocol.browser.driver

import ai.platon.pulsar.browser.driver.BrowserControl
import ai.platon.pulsar.browser.driver.chrome.*
import ai.platon.pulsar.browser.driver.chrome.util.ChromeDevToolsInvocationException
import ai.platon.pulsar.browser.driver.chrome.util.ChromeServiceException
import com.github.kklisura.cdt.protocol.types.page.CaptureScreenshotFormat
import com.github.kklisura.cdt.protocol.types.page.Viewport
import org.openqa.selenium.NoSuchSessionException
import org.openqa.selenium.OutputType
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.remote.SessionId
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.streams.toList

/**
 * TODO: more compatible methods with RemoteWebDriver, or disable them explicitly
 * */
class ChromeDevtoolsDriver(
        userAgent: String = "",
        browserControl: WebDriverControl,
        launchOptions: ChromeDevtoolsOptions,
        devToolsConfig: DevToolsConfig = DevToolsConfig()
): RemoteWebDriver() {

    companion object {
        private val log = LoggerFactory.getLogger(ChromeDevtoolsDriver::class.java)!!

        private var chromeProcessLaunched = false
        private val numInstances = AtomicInteger()
        private lateinit var launcher: ChromeLauncher
        private lateinit var chrome: RemoteChrome
        private val devToolsList = ConcurrentLinkedQueue<RemoteDevTools>()

        private fun launchChromeIfNecessary(launchOptions: ChromeDevtoolsOptions) {
            synchronized(ChromeLauncher::class.java) {
                if (!chromeProcessLaunched) {
                    launcher = ChromeLauncher(shutdownHookRegistry = ChromeDevtoolsDriverShutdownHookRegistry())
                    chrome = launcher.launch(launchOptions)
                    chromeProcessLaunched = true
                }
            }
        }

        private fun closeChromeIfNecessary() {
            if (chromeProcessLaunched) {
                synchronized(ChromeLauncher::class.java) {
                    if (chromeProcessLaunched && numInstances.get() == 0) {
                        chromeProcessLaunched = false

                        // require devToolsList.isEmpty()
                        devToolsList.toList().parallelStream().forEach { it.waitUntilClosed() }
                        devToolsList.clear()

                        chrome.use { it.close() }
                        launcher.use { it.close() }

                        checkChromeProcesses()
                    }
                }
            }
        }

        private fun checkChromeProcesses() {
            val process = ProcessLauncher().launch("ps", listOf("-efw"))
            val runningChromes = BufferedReader(InputStreamReader(process.inputStream)).use {
                reader -> reader.lines().filter { it.contains("chrome.+--headless".toRegex()) }.toList()
            }

            if (runningChromes.isNotEmpty()) {
                log.warn("There are still {} running chrome processes after closing", runningChromes.size)
            }
        }
    }

    private val clientLibJs = browserControl.parseLibJs(false)
    var pageLoadTimeout = browserControl.pageLoadTimeout
    var scriptTimeout = browserControl.scriptTimeout
    var scrollDownCount = browserControl.scrollDownCount
    var scrollInterval = browserControl.scrollInterval

    private var tab: ChromeTab
    private var devTools: RemoteDevTools

    private var lastSessionId: SessionId? = null
    private val browser get() = devTools.browser
    private var navigateUrl = ""
    private val page get() = devTools.page
    private val mainFrame get() = page.frameTree.frame
    private val network get() = devTools.network
    private val fetch get() = devTools.fetch
    private val runtime get() = devTools.runtime
    private val emulation get() = devTools.emulation

    private val numSessionLost = AtomicInteger()
    private val closed = AtomicBoolean()
    private val isGone get() = closed.get() || !devTools.isOpen || numSessionLost.get() > 1

    val viewport: Viewport
        get() {
            val viewport = Viewport()
            viewport.x = 0.0
            viewport.y = 0.0
            viewport.width = BrowserControl.viewPort.getWidth()
            viewport.height = BrowserControl.viewPort.getHeight()
            viewport.scale = 1.0
            return viewport
        }

    init {
        try {
            launchChromeIfNecessary(launchOptions)

            // In chrome every tab is a separate process
            tab = chrome.createTab()
            navigateUrl = tab.url?:""

            devTools = chrome.createDevTools(tab, devToolsConfig)
            devToolsList.add(devTools)

            if (userAgent.isNotEmpty()) {
                emulation.setUserAgentOverride(userAgent)
            }

            numInstances.incrementAndGet()
        } catch (t: Throwable) {
            throw ChromeServiceException("Failed to create chrome devtools driver", t)
        }
    }

    @Throws(NoSuchSessionException::class)
    override fun get(url: String) {
        if (isGone) return

        try {
            page.addScriptToEvaluateOnNewDocument(clientLibJs)
//        page.onDomContentEventFired { event: DomContentEventFired ->
//            // The page's main html content is ready, but css/js are not ready, document.readyState === 'interactive'
//            // runtime.evaluate("__utils__.checkPulsarStatus()")
//        }
//
//        page.onLoadEventFired { event: LoadEventFired ->
//            simulate()
//        }

            // block urls by url pattern
//            if ("imagesEnabled" in launchOptions.additionalArguments.keys) {
//
//            }
            network.setBlockedURLs(listOf("*.png", "*.jpg", "*.gif", "*.ico"))

            // Log requests with onRequestWillBeSent event handler
//            network.onRequestWillBeSent { event: RequestWillBeSent ->
//                if (event.type == ResourceType.IMAGE) {
//                    // TODO: fetch is not supported?
//                    // fetch.failRequest(event.requestId, ErrorReason.BLOCKED_BY_CLIENT)
//                    // println(event.request.url)
//                }
//            }

            page.enable()
            network.enable()
//            fetch.enable()

            navigateUrl = url
            page.navigate(url)
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    @Throws(NoSuchSessionException::class)
    suspend fun asyncGet(url: String) {
        if (isGone) return

        try {
            page.addScriptToEvaluateOnNewDocument(clientLibJs)
            network.setBlockedURLs(listOf("*.png", "*.jpg", "*.gif", "*.ico"))

            page.enable()
            network.enable()

            navigateUrl = url
            // use asynchronous api
            page.navigate(url)
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    @Throws(NoSuchSessionException::class)
    fun stopLoading() {
        if (isGone) return

        try {
            page.stopLoading()
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    @Throws(NoSuchSessionException::class)
    fun evaluate(expression: String): Any? {
        if (isGone) return null

        try {
            val evaluate = runtime.evaluate(expression)
            val result = evaluate?.result
            // TODO: handle errors here
            return result?.value
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    @Throws(NoSuchSessionException::class)
    override fun executeScript(script: String, vararg args: Any): Any? {
        TODO("Use evaluate instead")
    }

    @Throws(NoSuchSessionException::class)
    override fun getSessionId(): SessionId? {
        try {
            lastSessionId = if (isGone) null else SessionId(mainFrame.id)
            return lastSessionId
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    @Throws(NoSuchSessionException::class)
    override fun getCurrentUrl(): String {
        try {
            navigateUrl = if (isGone) navigateUrl else mainFrame.url
            return navigateUrl
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    @Throws(NoSuchSessionException::class)
    override fun getWindowHandles(): Set<String> {
        if (isGone) return setOf()
        return chrome.getTabs().mapTo(HashSet()) { it.id }
    }

    @Throws(NoSuchSessionException::class)
    override fun getPageSource(): String {
        try {
            val evaluate = runtime.evaluate("document.documentElement.outerHTML")
            return evaluate?.result?.value?.toString()?:""
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    @Throws(NoSuchSessionException::class)
    fun getCookieNames(): List<String> {
        if (isGone) return listOf()

        try {
            return devTools.network.allCookies.map { it.name }
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    @Throws(NoSuchSessionException::class)
    fun deleteAllCookies() {
        if (isGone) return

        try {
            devTools.network.clearBrowserCookies()
            devTools.network.clearBrowserCache()
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    @Throws(NoSuchSessionException::class)
    fun deleteCookieNamed(name: String) {
        if (isGone) return

        try {
            devTools.network.deleteCookies(name)
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

//    fun deleteLocalStorage() {
//        // devTools.cacheStorage.deleteCache()
//    }

    @Throws(NoSuchSessionException::class)
    override fun <X : Any> getScreenshotAs(outputType: OutputType<X>): X {
        try {
            val result = page.captureScreenshot(CaptureScreenshotFormat.PNG, 100, viewport, true)
            return outputType.convertFromBase64Png(result)
        } catch (e: ChromeDevToolsInvocationException) {
            numSessionLost.incrementAndGet()
            throw NoSuchSessionException(e.message)
        }
    }

    override fun toString(): String {
        return "Chrome Devtools Driver ($lastSessionId)"
    }

    /**
     * Quit browser
     * */
    override fun quit() {
        close()
        closeChromeIfNecessary()
    }

    /**
     * Close the tab hold by this driver
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            devTools.use { it.close() }
            numInstances.decrementAndGet()
        }
    }

    @Throws(NoSuchSessionException::class)
    private fun isMainFrame(frameId: String): Boolean {
        if (isGone) return false

        return mainFrame.id == frameId
    }

    class ChromeDevtoolsDriverShutdownHookRegistry : ChromeLauncher.ShutdownHookRegistry {

        override fun register(thread: Thread) {
        }

        override fun remove(thread: Thread) {
        }
    }
}
