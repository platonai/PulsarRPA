package ai.platon.pulsar.agentic.support

import ai.platon.pulsar.agentic.ai.support.ToolCallExecutor
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.jsoup.Connection
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration

class ToolCallExecutorCoverageTest {
    private val executor = ToolCallExecutor()

    @Test
    fun `waitForSelector and waitForPage`() = runBlocking {
        val driver = mockk<WebDriver>(relaxed = true)
        coEvery { driver.waitForSelector(any<String>()) } returns Duration.ofMillis(999)
        coEvery { driver.waitForSelector(any<String>(), any<Long>()) } returns 123L
        coEvery { driver.waitForPage(any(), any()) } returns null

        assertEquals(Duration.ofMillis(999), executor.execute("driver.waitForSelector(\"#a\")", driver))
        assertEquals(123L, executor.execute("driver.waitForSelector(\"#a\", 123)", driver))

        assertNull(executor.execute("driver.waitForPage(\"https://u\")", driver))
        assertNull(executor.execute("driver.waitForPage(\"https://u\", 321)", driver))
        coVerify { driver.waitForPage("https://u", Duration.ofMillis(30000)) }
        coVerify { driver.waitForPage("https://u", Duration.ofMillis(321)) }
    }

    @Test
    fun `page info getters`() = runBlocking {
        val driver = mockk<WebDriver>(relaxed = true)
        coEvery { driver.currentUrl() } returns "cu"
        coEvery { driver.url() } returns "du"
        coEvery { driver.documentURI() } returns "doc"
        coEvery { driver.baseURI() } returns "base"
        coEvery { driver.referrer() } returns "ref"
        coEvery { driver.pageSource() } returns "<html/>"

        assertEquals("cu", executor.execute("driver.currentUrl()", driver))
        assertEquals("du", executor.execute("driver.url()", driver))
        assertEquals("doc", executor.execute("driver.documentURI()", driver))
        assertEquals("base", executor.execute("driver.baseURI()", driver))
        assertEquals("ref", executor.execute("driver.referrer()", driver))
        assertEquals("<html/>", executor.execute("driver.pageSource()", driver))
    }

    @Test
    fun `outerHTML clickablePoint boundingBox`() = runBlocking {
        val driver = mockk<WebDriver>(relaxed = true)
        coEvery { driver.outerHTML() } returns "<html/>"
        coEvery { driver.outerHTML(any()) } returns "<div/>"
        coEvery { driver.clickablePoint(any()) } returns null
        coEvery { driver.boundingBox(any()) } returns null

        assertEquals("<html/>", executor.execute("driver.outerHTML()", driver))
        assertEquals("<div/>", executor.execute("driver.outerHTML(\"#a\")", driver))
        assertNull(executor.execute("driver.clickablePoint(\"#a\")", driver))
        assertNull(executor.execute("driver.boundingBox(\"#a\")", driver))
    }

    @Test
    fun `cookies apis`() = runBlocking {
        val driver = mockk<WebDriver>(relaxed = true)
        coEvery { driver.getCookies() } returns emptyList()
        coEvery { driver.deleteCookies(any(), any(), any(), any()) } just Runs
        coEvery { driver.clearBrowserCookies() } just Runs

        assertEquals(emptyList<Any>(), executor.execute("driver.getCookies()", driver))
        assertNull(executor.execute("driver.deleteCookies(\"n\", \"u\", \"d\", \"p\")", driver))
        coVerify { driver.deleteCookies("n", "u", "d", "p") }
        assertNull(executor.execute("driver.clearBrowserCookies()", driver))
    }

    @Test
    fun `jsoup and raw resource loaders`() = runBlocking {
        val driver = mockk<WebDriver>(relaxed = true)
        val conn = mockk<Connection>()
        val resp = mockk<Connection.Response>()
        val net = mockk<ai.platon.pulsar.browser.driver.chrome.NetworkResourceResponse>()
        coEvery { driver.newJsoupSession() } returns conn
        coEvery { driver.loadJsoupResource(any()) } returns resp
        coEvery { driver.loadResource(any()) } returns net

        assertSame(conn, executor.execute("driver.newJsoupSession()", driver))
        assertSame(resp, executor.execute("driver.loadJsoupResource(\"https://u\")", driver))
        assertSame(net, executor.execute("driver.loadResource(\"https://u\")", driver))
    }

    @Test
    fun `pause stop and visibility aliases`() = runBlocking {
        val driver = mockk<WebDriver>(relaxed = true)
        coEvery { driver.pause() } just Runs
        coEvery { driver.stop() } just Runs
        coEvery { driver.visible(any()) } returns true
        coEvery { driver.isHidden(any()) } returns false
        coEvery { driver.isChecked(any()) } returns true

        assertNull(executor.execute("driver.pause()", driver))
        assertNull(executor.execute("driver.stop()", driver))
        assertEquals(true, executor.execute("driver.visible(\"#a\")", driver))
        assertEquals(false, executor.execute("driver.isHidden(\"#a\")", driver))
        assertEquals(true, executor.execute("driver.isChecked(\"#a\")", driver))
    }

    @Test
    fun `invalid expression or unknown method returns null`() = runBlocking {
        val driver = mockk<WebDriver>(relaxed = true)
        assertNull(executor.execute("driver.unknown(1,2,3)", driver))
        assertNull(executor.execute("driver.open(\"unterminated)", driver))
    }
}

