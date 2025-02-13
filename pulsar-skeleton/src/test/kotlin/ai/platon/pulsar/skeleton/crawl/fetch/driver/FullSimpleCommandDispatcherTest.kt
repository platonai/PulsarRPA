import ai.platon.pulsar.skeleton.crawl.fetch.driver.SimpleCommandDispatcher
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.test.Ignore

@Ignore("Full simple command dispatcher not implemented yet")
class FullSimpleCommandDispatcherTest {

    @Test
    fun testParseSimpleFunctionCall_addInitScript() {
        val input = "driver.addInitScript(\"script\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("addInitScript", result?.second)
        assertEquals(listOf("script"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_addBlockedURLs() {
        val input = "driver.addBlockedURLs(listOf(\"url1\", \"url2\"))"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("addBlockedURLs", result?.second)
        assertEquals(listOf("listOf(\"url1\", \"url2\")"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_addProbabilityBlockedURLs() {
        val input = "driver.addProbabilityBlockedURLs(listOf(\"regex1\", \"regex2\"))"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("addProbabilityBlockedURLs", result?.second)
        assertEquals(listOf("listOf(\"regex1\", \"regex2\")"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_setTimeouts() {
        val input = "driver.setTimeouts(browserSettings)"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("setTimeouts", result?.second)
        assertEquals(listOf("browserSettings"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_open() {
        val input = "driver.open(\"https://www.example.com\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("open", result?.second)
        assertEquals(listOf("https://www.example.com"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_navigateTo_url() {
        val input = "driver.navigateTo(\"https://www.example.com\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("navigateTo", result?.second)
        assertEquals(listOf("https://www.example.com"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_navigateTo_entry() {
        val input = "driver.navigateTo(entry)"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("navigateTo", result?.second)
        assertEquals(listOf("entry"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_currentUrl() {
        val input = "driver.currentUrl()"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("currentUrl", result?.second)
        assertTrue(result?.third?.isEmpty() ?: false)
    }

    @Test
    fun testParseSimpleFunctionCall_url() {
        val input = "driver.url()"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("url", result?.second)
        assertTrue(result?.third?.isEmpty() ?: false)
    }

    @Test
    fun testParseSimpleFunctionCall_documentURI() {
        val input = "driver.documentURI()"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("documentURI", result?.second)
        assertTrue(result?.third?.isEmpty() ?: false)
    }

    @Test
    fun testParseSimpleFunctionCall_baseURI() {
        val input = "driver.baseURI()"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("baseURI", result?.second)
        assertTrue(result?.third?.isEmpty() ?: false)
    }

    @Test
    fun testParseSimpleFunctionCall_referrer() {
        val input = "driver.referrer()"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("referrer", result?.second)
        assertTrue(result?.third?.isEmpty() ?: false)
    }

    @Test
    fun testParseSimpleFunctionCall_pageSource() {
        val input = "driver.pageSource()"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("pageSource", result?.second)
        assertTrue(result?.third?.isEmpty() ?: false)
    }

    @Test
    fun testParseSimpleFunctionCall_getCookies() {
        val input = "driver.getCookies()"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("getCookies", result?.second)
        assertTrue(result?.third?.isEmpty() ?: false)
    }

    @Test
    fun testParseSimpleFunctionCall_deleteCookies_name() {
        val input = "driver.deleteCookies(\"name\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("deleteCookies", result?.second)
        assertEquals(listOf("name"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_deleteCookies_name_url() {
        val input = "driver.deleteCookies(\"name\", \"https://www.example.com\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("deleteCookies", result?.second)
        assertEquals(listOf("name", "https://www.example.com"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_deleteCookies_name_url_domain_path() {
        val input = "driver.deleteCookies(\"name\", \"https://www.example.com\", \"domain\", \"path\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("deleteCookies", result?.second)
        assertEquals(listOf("name", "https://www.example.com", "domain", "path"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_clearBrowserCookies() {
        val input = "driver.clearBrowserCookies()"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("clearBrowserCookies", result?.second)
        assertTrue(result?.third?.isEmpty() ?: false)
    }

    @Test
    fun testParseSimpleFunctionCall_waitForSelector_selector() {
        val input = "driver.waitForSelector(\"h2.title\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("waitForSelector", result?.second)
        assertEquals(listOf("h2.title"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_waitForSelector_selector_timeoutMillis() {
        val input = "driver.waitForSelector(\"h2.title\", 30000)"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("waitForSelector", result?.second)
        assertEquals(listOf("h2.title", "30000"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_waitForSelector_selector_timeout() {
        val input = "driver.waitForSelector(\"h2.title\", Duration.ofSeconds(30))"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("waitForSelector", result?.second)
        assertEquals(listOf("h2.title", "Duration.ofSeconds(30)"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_waitForSelector_selector_action() {
        val input = "driver.waitForSelector(\"h2.title\") { driver.scrollDown() }"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("waitForSelector", result?.second)
        assertEquals(listOf("h2.title", "{ driver.scrollDown() }"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_waitForSelector_selector_timeoutMillis_action() {
        val input = "driver.waitForSelector(\"h2.title\", 30000) { driver.scrollDown() }"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("waitForSelector", result?.second)
        assertEquals(listOf("h2.title", "30000", "{ driver.scrollDown() }"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_waitForSelector_selector_timeout_action() {
        val input = "driver.waitForSelector(\"h2.title\", Duration.ofSeconds(30)) { driver.scrollDown() }"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("waitForSelector", result?.second)
        assertEquals(listOf("h2.title", "Duration.ofSeconds(30)", "{ driver.scrollDown() }"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_waitForNavigation() {
        val input = "driver.waitForNavigation()"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("waitForNavigation", result?.second)
        assertTrue(result?.third?.isEmpty() ?: false)
    }

    @Test
    fun testParseSimpleFunctionCall_waitForNavigation_oldUrl() {
        val input = "driver.waitForNavigation(\"https://www.example.com\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("waitForNavigation", result?.second)
        assertEquals(listOf("https://www.example.com"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_waitForNavigation_oldUrl_timeoutMillis() {
        val input = "driver.waitForNavigation(\"https://www.example.com\", 1000)"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("waitForNavigation", result?.second)
        assertEquals(listOf("https://www.example.com", "1000"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_waitForNavigation_oldUrl_timeout() {
        val input = "driver.waitForNavigation(\"https://www.example.com\", Duration.ofSeconds(30))"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("waitForNavigation", result?.second)
        assertEquals(listOf("https://www.example.com", "Duration.ofSeconds(30)"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_waitForPage() {
        val input = "driver.waitForPage(\"https://www.example.com\", Duration.ofSeconds(30))"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("waitForPage", result?.second)
        assertEquals(listOf("https://www.example.com", "Duration.ofSeconds(30)"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_waitUntil_predicate() {
        val input = "driver.waitUntil { driver.exists(\"h2.title\") }"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("waitUntil", result?.second)
        assertEquals(listOf("{ driver.exists(\"h2.title\") }"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_waitUntil_timeoutMillis_predicate() {
        val input = "driver.waitUntil(10000) { driver.exists(\"h2.title\") }"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("waitUntil", result?.second)
        assertEquals(listOf("10000", "{ driver.exists(\"h2.title\") }"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_waitUntil_timeout_predicate() {
        val input = "driver.waitUntil(Duration.ofSeconds(10)) { driver.exists(\"h2.title\") }"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("waitUntil", result?.second)
        assertEquals(listOf("Duration.ofSeconds(10)", "{ driver.exists(\"h2.title\") }"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_exists() {
        val input = "driver.exists(\"h2.title\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("exists", result?.second)
        assertEquals(listOf("h2.title"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_isHidden() {
        val input = "driver.isHidden(\"input[name='q']\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("isHidden", result?.second)
        assertEquals(listOf("input[name='q']"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_isVisible() {
        val input = "driver.isVisible(\"input[name='q']\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("isVisible", result?.second)
        assertEquals(listOf("input[name='q']"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_visible() {
        val input = "driver.visible(\"input[name='q']\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("visible", result?.second)
        assertEquals(listOf("input[name='q']"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_isChecked() {
        val input = "driver.isChecked(\"input[name='agree']\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("isChecked", result?.second)
        assertEquals(listOf("input[name='agree']"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_bringToFront() {
        val input = "driver.bringToFront()"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("bringToFront", result?.second)
        assertTrue(result?.third?.isEmpty() ?: false)
    }

    @Test
    fun testParseSimpleFunctionCall_focus() {
        val input = "driver.focus(\"input[name='q']\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("focus", result?.second)
        assertEquals(listOf("input[name='q']"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_type() {
        val input = "driver.type(\"input[name='q']\", \"Hello, World!\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("type", result?.second)
        assertEquals(listOf("input[name='q']", "\"Hello, World!\""), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_fill() {
        val input = "driver.fill(\"input[name='q']\", \"Hello, World!\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("fill", result?.second)
        assertEquals(listOf("input[name='q']", "\"Hello, World!\""), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_press() {
        val input = "driver.press(\"input[name='q']\", \"Enter\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("press", result?.second)
        assertEquals(listOf("input[name='q']", "\"Enter\""), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_click() {
        val input = "driver.click(\"button[type='submit']\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("click", result?.second)
        assertEquals(listOf("button[type='submit']"), result?.third)
    }

    @Test
    fun testParseSimpleFunctionCall_clickTextMatches() {
        val input = "driver.clickTextMatches(\"button\", \"submit\")"
        val result = SimpleCommandDispatcher.parseSimpleFunctionCall(input)
        assertNotNull(result)
        assertEquals("driver", result?.first)
        assertEquals("clickTextMatches", result?.second)
        assertEquals(listOf("button", "\"submit\""), result?.third)
    }
}
