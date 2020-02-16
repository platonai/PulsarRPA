package ai.platon.pulsar.net.browser

import ai.platon.pulsar.browser.driver.BrowserControl
import ai.platon.pulsar.browser.driver.chrome.*
import com.github.kklisura.cdt.protocol.events.network.RequestWillBeSent
import com.github.kklisura.cdt.protocol.types.network.ErrorReason
import com.github.kklisura.cdt.protocol.types.network.ResourceType
import com.github.kklisura.cdt.protocol.types.page.CaptureScreenshotFormat
import com.github.kklisura.cdt.protocol.types.page.Viewport
import org.openqa.selenium.NoSuchSessionException
import org.openqa.selenium.OutputType
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.remote.SessionId
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.streams.toList

/**
 * TODO: more compatible with RemoteWebDriver
 * */
class ChromeDevtoolsDriver(
        private val userAgent: String = "",
        private val browserControl: WebDriverControl,
        private val launchOptions: ChromeDevtoolsOptions
): RemoteWebDriver() {

    companion object {
        private val log = LoggerFactory.getLogger(ChromeDevtoolsDriver::class.java)!!

        private var chromeProcessLaunched = false
        private val numInstances = AtomicInteger()
        private lateinit var launcher: ChromeLauncher
        private lateinit var chrome: ChromeService
        private val tabs = mutableMapOf<String, ChromeTab>()

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
            synchronized(ChromeLauncher::class.java) {
                if (chromeProcessLaunched) {
                    chrome.use { it.close() }
                    launcher.use { it.close() }
                    chromeProcessLaunched = false

                    checkChromeProcesses()
                }
            }
        }

        private fun checkChromeProcesses() {
            val process = ProcessLauncher().launch("ps", listOf("-efw"))
            val runningChromes = BufferedReader(InputStreamReader(process.inputStream)).use {
                reader -> reader.lines().filter { it.contains("chrome.+headless".toRegex()) }.toList()
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
    private var devTools: ChromeDevToolsService

    private val browser get() = devTools.browser
    private val page get() = devTools.page
    private val mainFrame get() = page.frameTree.frame
    private val network get() = devTools.network
    private val fetch get() = devTools.fetch
    private val runtime get() = devTools.runtime
    private val emulation get() = devTools.emulation
    private val pageLock = ReentrantLock()
    private val pageCondition = pageLock.newCondition()
    private val closed = AtomicBoolean()
    val isClosed get() = closed.get()

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
        launchChromeIfNecessary(launchOptions)

        // In chrome every tab is a separate process
        tab = chrome.createTab()
        tabs[tab.id] = tab

        devTools = chrome.createDevToolsService(tab)
        if (userAgent.isNotEmpty()) {
            emulation.setUserAgentOverride(userAgent)
        }

        numInstances.incrementAndGet()
    }

    override fun get(url: String) {
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
            // network.setBlockedURLs(listOf("*.png", "*.jpg", "*.gif", "*.ico"))

            // Log requests with onRequestWillBeSent event handler
            network.onRequestWillBeSent { event: RequestWillBeSent ->
                if (event.type == ResourceType.IMAGE) {
                    // TODO: fetch is not supported?
                    // fetch.failRequest(event.requestId, ErrorReason.BLOCKED_BY_CLIENT)
                    // println(event.request.url)
                }
            }

            page.enable()
            network.enable()
//        fetch.enable()

            page.navigate(url)
        } catch (e: ChromeDevToolsInvocationException) {
            throw NoSuchSessionException(e.message)
        }
    }

    fun evaluate(expression: String): Any? {
        try {
            val evaluate = runtime.evaluate(expression)
            val result = evaluate.result
            // TODO: handle errors here
            return result.value
        } catch (e: ChromeDevToolsInvocationException) {
            throw NoSuchSessionException(e.message)
        }
    }

    override fun executeScript(script: String, vararg args: Any): Any? {
        TODO("Use evaluate instead")
    }

    override fun getSessionId(): SessionId? {
        return try {
            if (isClosed) null else SessionId(mainFrame.id)
        } catch (e: ChromeDevToolsInvocationException) {
            null
        }
    }

    override fun getCurrentUrl(): String {
        try {
            return mainFrame.url
        } catch (e: ChromeDevToolsInvocationException) {
            throw NoSuchSessionException(e.message)
        }
    }

    override fun getWindowHandles(): Set<String> {
        return chrome.getTabs().mapTo(HashSet()) { it.id }
    }

    override fun getPageSource(): String {
        try {
            val evaluate = runtime.evaluate("document.documentElement.outerHTML")
            return evaluate.result.value.toString()
        } catch (e: ChromeDevToolsInvocationException) {
            throw NoSuchSessionException(e.message)
        }
    }

    fun getCookieNames(): List<String> {
        try {
            return devTools.network.allCookies.map { it.name }
        } catch (e: ChromeDevToolsInvocationException) {
            throw NoSuchSessionException(e.message)
        }
    }

    fun deleteAllCookies() {
        try {
            devTools.network.clearBrowserCookies()
            devTools.network.clearBrowserCache()
        } catch (e: ChromeDevToolsInvocationException) {
            throw NoSuchSessionException(e.message)
        }
    }

    fun deleteCookieNamed(name: String) {
        try {
            devTools.network.deleteCookies(name)
        } catch (e: ChromeDevToolsInvocationException) {
            throw NoSuchSessionException(e.message)
        }
    }

//    fun deleteLocalStorage() {
//        // devTools.cacheStorage.deleteCache()
//    }

    override fun <X : Any> getScreenshotAs(outputType: OutputType<X>): X {
        try {
            val result = page.captureScreenshot(CaptureScreenshotFormat.PNG, 100, viewport, true)
            return outputType.convertFromBase64Png(result)
        } catch (e: ChromeDevToolsInvocationException) {
            throw NoSuchSessionException(e.message)
        }
    }

    override fun toString(): String {
        return "Chrome Devtools Driver ($sessionId)"
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
            tabs.remove(tab.id)
            devTools.use { it.close() }
        }
    }

    private fun isMainFrame(frameId: String): Boolean {
        return mainFrame.id == frameId
    }

    class ChromeDevtoolsDriverShutdownHookRegistry : ChromeLauncher.ShutdownHookRegistry {

        override fun register(thread: Thread) {
        }

        override fun remove(thread: Thread) {
        }
    }
}
