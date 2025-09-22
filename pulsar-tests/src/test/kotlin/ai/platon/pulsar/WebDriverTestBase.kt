package ai.platon.pulsar

import ai.platon.pulsar.browser.WebDriverService
import ai.platon.pulsar.browser.common.BrowserSettings
import ai.platon.pulsar.browser.common.SimpleScriptConfuser
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.persist.model.ActiveDOMMetadata
import ai.platon.pulsar.protocol.browser.impl.DefaultBrowserFactory
import ai.platon.pulsar.ql.context.SQLContexts
import ai.platon.pulsar.skeleton.context.PulsarContexts
import ai.platon.pulsar.skeleton.crawl.fetch.driver.Browser
import ai.platon.pulsar.skeleton.crawl.fetch.driver.BrowserFactory
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import ai.platon.pulsar.skeleton.session.PulsarSession
import ai.platon.pulsar.util.server.Application
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertNotNull

@SpringBootTest(classes = [Application::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class WebDriverTestBase(
    override val session: PulsarSession = SQLContexts.getOrCreateSession()
) : TestWebSiteAccess(session) {

    companion object {
        val isInitialized = AtomicBoolean(false)
        lateinit var browser: Browser

        @JvmStatic
        @AfterAll
        fun closeBrowser() {
            if (isInitialized.compareAndSet(true, false)) {
                browser.close()
            }
        }
    }

    @BeforeEach
    fun initBrowser() {
        synchronized(isInitialized) {
            if (isInitialized.compareAndSet(false, true)) {
                browser = browserFactory.launchRandomTempBrowser()
                browser.newDriver()
            }
        }
    }

    val browserFactory get() = context.getBeanOrNull(BrowserFactory::class) ?: DefaultBrowserFactory(session.unmodifiedConfig)

    open val webDriverService get() = WebDriverService(browserFactory)

//    val browser get() = webDriverService.browser

    protected val expressions = """
            typeof(window)
            
            typeof(window.history)
            window.history
            window.history.length
            
            typeof(document)
            document.location
            document.baseURI
            
            typeof(document.body)
            document.body.clientWidth
            
            typeof(__pulsar_)
            __pulsar_utils__.add(1, 1)
        """.trimIndent().split("\n").map { it.trim() }.filter { it.isNotBlank() }

    val settings get() = BrowserSettings(session.sessionConfig)
    val confuser get() = settings.confuser as SimpleScriptConfuser

    suspend fun evaluateExpressions(driver: WebDriver, type: String) {
        expressions.forEach { expression ->
            val detail = driver.evaluateDetail(expression)
            println(String.format("%-6s%-40s%s", type, expression, detail))
        }
    }

    protected fun runWebDriverTest(url: String, browser: Browser, block: suspend (driver: WebDriver) -> Unit) =
        webDriverService.runWebDriverTest(url, browser, block)

    protected fun runResourceWebDriverTest(url: String, block: suspend (driver: WebDriver) -> Unit) =
        webDriverService.runResourceWebDriverTest(url, block)

    protected fun runResourceWebDriverTest(url: String, browser: Browser, block: suspend (driver: WebDriver) -> Unit) =
        webDriverService.runResourceWebDriverTest(url, browser, block)

    protected fun runWebDriverTest(block: suspend (driver: WebDriver) -> Unit) = webDriverService.runWebDriverTest(block)

    protected fun runWebDriverTest(browserId: BrowserId, block: suspend (driver: WebDriver) -> Unit) =
        webDriverService.runWebDriverTest(browserId, block)

    protected fun runWebDriverTest(browser: Browser, block: suspend (driver: WebDriver) -> Unit) =
        webDriverService.runWebDriverTest(browser, block)

    protected suspend fun open(url: String, driver: WebDriver, scrollCount: Int = 3) = webDriverService.open(url, driver, scrollCount)

    protected suspend fun openResource(url: String, driver: WebDriver, scrollCount: Int = 1) = webDriverService.openResource(url, driver, scrollCount)

    protected suspend fun computeActiveDOMMetadata(driver: WebDriver): ActiveDOMMetadata {
        val detail = driver.evaluateDetail("JSON.stringify(__pulsar_utils__.computeMetadata())")
        println(detail)
        assertNotNull(detail)
        assertNotNull(detail.value)
        println(detail.value)
        val data = requireNotNull(detail.value?.toString())
        return pulsarObjectMapper().readValue(data)
    }
}