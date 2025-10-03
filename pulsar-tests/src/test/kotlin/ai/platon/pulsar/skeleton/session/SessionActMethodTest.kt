package ai.platon.pulsar.skeleton.session

import ai.platon.pulsar.skeleton.PulsarSettings
import ai.platon.pulsar.skeleton.ai.tta.ActionOptions
import ai.platon.pulsar.skeleton.ai.tta.TextToActionTestBase
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.util.server.EnabledMockServerApplication
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Tag("ExternalServiceTest")
@SpringBootTest(classes = [EnabledMockServerApplication::class], webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class SessionActMethodTest : TextToActionTestBase() {

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            PulsarSettings.withSPA()
        }
    }

    private val demoUrl = "http://localhost:18080/generated/tta/act/act-demo.html"

    // Resources initialized per test
    private lateinit var driver: WebDriver

    @BeforeEach
    fun setUp() {
        runBlocking {
            driver = newBoundDriver()
            // Open demo page so each test begins from consistent state
            session.open(demoUrl)
        }
    }

    @AfterEach
    fun tearDown() {
    }

    /**
     * Utility to wait for a condition with a timeout, reduces flakiness of dynamic actions.
     */
    private suspend fun waitUntil(timeoutMs: Long = 6000, intervalMs: Long = 200, condition: suspend () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(intervalMs)
        }
        return false
    }

    private fun newBoundDriver() = browser.newDriver().also { session.bindDriver(it) }

    /**
     * Mirrors step: open URL and basic parse / extract title.
     */
    @Test
    fun testOpenAndParseDemoPage() = runBlocking {
        val page = session.open(demoUrl)
        val document = session.parse(page)
        val title = document.selectFirstTextOrNull("#title")
        assertEquals("Session Instructions Demo", title, "Initial #title text should match")
    }

    /**
     * Mirrors action: "search for 'browser'" and validates dynamic results appear.
     */
    @Test
    fun testSearchActionShowsResults() = runBlocking {
        session.act(ActionOptions("search for 'browser'"))
        val appeared = waitUntil { driver.selectFirstTextOrNull("#searchResults")?.contains("browser") == true }
        assertTrue(appeared, "Search results should appear and contain query text 'browser'")
    }

    /**
     * Mirrors action: "click the 3rd link" which should navigate to pageC.html with title Page C (Third Link Target)
     */
    @Test
    fun testClickThirdLinkNavigatesToPageC() = runBlocking {
        session.act(ActionOptions("click the 3rd link"))
        val navigated = waitUntil { driver.currentUrl().endsWith("pageC.html") }
        assertTrue(navigated, "Should navigate to pageC.html after clicking 3rd link")
        val page = session.attach(driver.currentUrl(), driver)
        val doc = session.parse(page)
        assertEquals("Page C (Third Link Target)", doc.selectFirstTextOrNull("#title"))
    }

    /**
     * Mirrors action: "click the first link that contains 'Show HN' or 'Ask HN'" expecting page2.html.
     */
    @Test
    fun testClickShowHNLikeLink() = runBlocking {
        session.act(ActionOptions("click the first link that contains 'Show HN' or 'Ask HN'"))
        val navigated = waitUntil { driver.currentUrl().endsWith("page2.html") }
        assertTrue(navigated, "Should navigate to page2.html (Show HN: Demo Project)")
        val page = session.attach(driver.currentUrl(), driver)
        val doc = session.parse(page)
        assertEquals("Show HN: Demo Project", doc.selectFirstTextOrNull("#title"))
    }

    /**
     * Mirrors actions: click 3rd link -> navigate back -> navigate forward.
     */
    @Test
    fun testNavigationBackAndForward() = runBlocking {
        session.act(ActionOptions("click the 3rd link"))
        assertTrue(waitUntil { driver.currentUrl().endsWith("pageC.html") }, "Should reach pageC.html")
        session.act(ActionOptions("navigate back"))
        assertTrue(waitUntil { driver.currentUrl().endsWith("act-demo.html") }, "Should navigate back to demo page")
        session.act(ActionOptions("navigate forward"))
        assertTrue(waitUntil { driver.currentUrl().endsWith("pageC.html") }, "Should navigate forward again to pageC.html")
    }

    /**
     * Mirrors action: "extract article titles and their hrefs from the main list".
     * Since the agent returns a WebDriverAgent with a history, we assert key tokens appear there.
     */
    @Test
    fun testExtractArticleTitles() = runBlocking {
        val agent = session.act(ActionOptions("extract article titles and their hrefs from the main list"))
        val history = agent.history.joinToString("\n")
        assertTrue(history.contains("Show HN") || history.contains("Ask HN"), "History should contain extracted article titles")
    }
}
