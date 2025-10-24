package ai.platon.pulsar.skeleton.crawl.fetch.driver

import ai.platon.pulsar.skeleton.ai.support.ToolCall
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration

class ToolCallExecutorExecuteTest {

    private val executor = ToolCallExecutor()

    @Test
    fun `open and navigateTo basic mappings`() = runBlocking {
        val driver = mockk<WebDriver>(relaxed = true)

        coEvery { driver.open(any()) } just Runs
        val r1 = executor.execute("driver.open(\"https://t.tt\")", driver)
        assertNull(r1) // open returns Unit -> null propagated
        coVerify(exactly = 1) { driver.open("https://t.tt") }

        coEvery { driver.navigateTo(any<String>()) } just Runs
        val r2 = executor.execute("driver.navigateTo(\"https://a.b\")", driver)
        assertNull(r2)
        coVerify(exactly = 1) { driver.navigateTo("https://a.b") }

        val slotEntry = slot<NavigateEntry>()
        coEvery { driver.navigateTo(capture(slotEntry)) } just Runs
        executor.execute("driver.navigateTo(\"https://u\", \"https://p\")", driver)
        assertEquals("https://u", slotEntry.captured.url)
        assertEquals("https://p", slotEntry.captured.pageUrl)
    }

    @Test
    fun `click type fill press with conversions`() = runBlocking {
        val driver = mockk<WebDriver>(relaxed = true)

        coEvery { driver.click(any(), any()) } just Runs
        executor.execute("driver.click(\"#btn\")", driver)
        coVerify { driver.click("#btn", any()) }
        executor.execute("driver.click(\"#btn\", 3)", driver)
        coVerify { driver.click("#btn", 3) }

        coEvery { driver.type(any(), any()) } just Runs
        executor.execute("driver.type(\"#in\", \"abc\")", driver)
        coVerify { driver.type("#in", "abc") }

        coEvery { driver.fill(any(), any()) } just Runs
        executor.execute("driver.fill(\"#in\", \"xyz\")", driver)
        coVerify { driver.fill("#in", "xyz") }

        coEvery { driver.press(any(), any()) } just Runs
        executor.execute("driver.press(\"#in\", \"Enter\")", driver)
        coVerify { driver.press("#in", "Enter") }
    }

    @Test
    fun `waitForNavigation overloads and return values`() = runBlocking {
        val driver = mockk<WebDriver>(relaxed = true)

        val d0 = Duration.ofMillis(123)
        coEvery { driver.waitForNavigation() } returns d0
        val r0 = executor.execute("driver.waitForNavigation()", driver)
        assertEquals(d0, r0)

        val d1 = Duration.ofMillis(456)
        coEvery { driver.waitForNavigation(any<String>()) } returns d1
        val r1 = executor.execute("driver.waitForNavigation(\"old\")", driver)
        assertEquals(d1, r1)

        coEvery { driver.waitForNavigation(any<String>(), any<Long>()) } returns 789L
        val r2 = executor.execute("driver.waitForNavigation(\"old\", 789)", driver)
        assertEquals(789L, r2)
    }

    @Test
    fun `screenshot with and without selector`() = runBlocking {
        val driver = mockk<WebDriver>(relaxed = true)
        coEvery { driver.captureScreenshot() } returns "img0"
        coEvery { driver.captureScreenshot(any<String>()) } returns "img1"

        assertEquals("img0", executor.execute("driver.captureScreenshot()", driver))
        assertEquals("img1", executor.execute("driver.captureScreenshot(\"#a\")", driver))
        coVerify { driver.captureScreenshot() }
        coVerify { driver.captureScreenshot("#a") }
    }

    @Test
    fun `mouse wheels and moveMouseTo conversions`() = runBlocking {
        val driver = mockk<WebDriver>(relaxed = true)
        coEvery { driver.mouseWheelDown(any(), any(), any(), any()) } just Runs
        coEvery { driver.mouseWheelUp(any(), any(), any(), any()) } just Runs
        coEvery { driver.moveMouseTo(any<Double>(), any<Double>()) } just Runs
        coEvery { driver.moveMouseTo(any<String>(), any(), any()) } just Runs

        executor.execute("driver.mouseWheelDown()", driver)
        coVerify { driver.mouseWheelDown(1, 0.0, 0.0, 0) }
        executor.execute("driver.mouseWheelDown(2)", driver)
        coVerify { driver.mouseWheelDown(2, any(), any(), any()) }
        executor.execute("driver.mouseWheelDown(3, 1.5)", driver)
        coVerify { driver.mouseWheelDown(3, 1.5, 0.0, 0) }
        executor.execute("driver.mouseWheelDown(4, 2.0, 3.0, 10)", driver)
        coVerify { driver.mouseWheelDown(4, 2.0, 3.0, 10) }

        executor.execute("driver.mouseWheelUp()", driver)
        coVerify { driver.mouseWheelUp(1, 0.0, 0.0, 0) }

        executor.execute("driver.moveMouseTo(11.1, 22.2)", driver)
        coVerify { driver.moveMouseTo(11.1, 22.2) }

        executor.execute("driver.moveMouseTo(\"#id\", 7)", driver)
        coVerify { driver.moveMouseTo("#id", 7, 0) }

        executor.execute("driver.moveMouseTo(\"#id\", 7, 3)", driver)
        coVerify { driver.moveMouseTo("#id", 7, 3) }
    }

    @Test
    fun `attribute and property selections with ranges`() = runBlocking {
        val driver = mockk<WebDriver>(relaxed = true)
        coEvery { driver.selectFirstAttributeOrNull(any(), any()) } returns "v"
        coEvery { driver.selectAttributes(any()) } returns mapOf("a" to "b")
        coEvery { driver.selectAttributeAll(any(), any(), any(), any()) } returns listOf("x")
        coEvery { driver.selectFirstPropertyValueOrNull(any(), any()) } returns "pv"
        coEvery { driver.selectPropertyValueAll(any(), any(), any(), any()) } returns listOf("p1", "p2")

        assertEquals("v", executor.execute("driver.selectFirstAttributeOrNull(\"#a\", \"href\")", driver))
        assertEquals(mapOf("a" to "b"), executor.execute("driver.selectAttributes(\"#a\")", driver))
        executor.execute("driver.selectAttributeAll(\"#a\", \"href\", 2, 10)", driver)
        coVerify { driver.selectAttributeAll("#a", "href", 2, 10) }

        assertEquals("pv", executor.execute("driver.selectFirstPropertyValueOrNull(\"#a\", \"value\")", driver))
        executor.execute("driver.selectPropertyValueAll(\"#a\", \"value\", 1, 100)", driver)
        coVerify { driver.selectPropertyValueAll("#a", "value", 1, 100) }
    }

    @Test
    fun `hyperlinks anchors images selections`() = runBlocking {
        val driver = mockk<WebDriver>(relaxed = true)
        coEvery { driver.selectHyperlinks(any()) } returns emptyList()
        coEvery { driver.selectAnchors(any()) } returns emptyList()
        coEvery { driver.selectImages(any()) } returns emptyList()

        executor.execute("driver.selectHyperlinks(\"a\")", driver)
        coVerify { driver.selectHyperlinks("a") }
        executor.execute("driver.selectHyperlinks(\"a\", 2, 9)", driver)
        coVerify { driver.selectHyperlinks("a", 2, 9) }

        executor.execute("driver.selectAnchors(\"a\")", driver)
        coVerify { driver.selectAnchors("a") }
        executor.execute("driver.selectAnchors(\"a\", 3, 8)", driver)
        coVerify { driver.selectAnchors("a", 3, 8) }

        executor.execute("driver.selectImages(\"img\")", driver)
        coVerify { driver.selectImages("img") }
        executor.execute("driver.selectImages(\"img\", 5, 7)", driver)
        coVerify { driver.selectImages("img", 5, 7) }
    }

    @Test
    fun `evaluate and value and details`() = runBlocking {
        val driver = mockk<WebDriver>(relaxed = true)
        coEvery { driver.evaluate(any<String>()) } returns 42
        coEvery { driver.evaluateValue(any<String>()) } returns mapOf("a" to 1)
        coEvery { driver.evaluateDetail(any()) } returns null
        coEvery { driver.evaluateValueDetail(any()) } returns null

        assertEquals(42, executor.execute("driver.evaluate(\"1+41\")", driver))
        assertEquals(mapOf("a" to 1), executor.execute("driver.evaluateValue(\"({a:1})\")", driver))
        assertNull(executor.execute("driver.evaluateDetail(\"1\")", driver))
        assertNull(executor.execute("driver.evaluateValueDetail(\"1\")", driver))
    }

    @Test
    fun `deleteCookies overload mapping`() = runBlocking {
        val driver = mockk<WebDriver>(relaxed = true)
        coEvery { driver.deleteCookies(any(), any(), any(), any()) } just Runs
        coEvery { driver.deleteCookies(any<String>(), any<String>()) } just Runs

        executor.execute("driver.deleteCookies(\"n\")", driver)
        coVerify { driver.deleteCookies("n", null, null, null) }

        executor.execute("driver.deleteCookies(\"n\", \"https://u\")", driver)
        coVerify { driver.deleteCookies("n", "https://u") }
    }

    @Test
    fun `exists visible focus and booleans`() = runBlocking {
        val driver = mockk<WebDriver>(relaxed = true)
        coEvery { driver.exists(any()) } returns true
        coEvery { driver.isVisible(any()) } returns false
        coEvery { driver.focus(any()) } just Runs

        assertEquals(true, executor.execute("driver.exists(\"#a\")", driver))
        assertEquals(false, executor.execute("driver.isVisible(\"#a\")", driver))
        assertNull(executor.execute("driver.focus(\"#a\")", driver))
    }

    @Test
    fun `scroll helpers and go history`() = runBlocking {
        val driver = mockk<WebDriver>(relaxed = true)
        coEvery { driver.scrollDown(any()) } just Runs
        coEvery { driver.scrollUp(any()) } just Runs
        coEvery { driver.scrollToTop() } just Runs
        coEvery { driver.scrollToBottom() } just Runs
        coEvery { driver.scrollToMiddle(any()) } just Runs
        coEvery { driver.scrollToScreen(any()) } just Runs
        coEvery { driver.goBack() } just Runs
        coEvery { driver.goForward() } just Runs

        executor.execute("driver.scrollDown()", driver)
        executor.execute("driver.scrollUp(3)", driver)
        executor.execute("driver.scrollToTop()", driver)
        executor.execute("driver.scrollToBottom()", driver)
        executor.execute("driver.scrollToMiddle(0.7)", driver)
        executor.execute("driver.scrollToScreen()", driver)
        executor.execute("driver.scrollToScreen(1.25)", driver)
        executor.execute("driver.goBack()", driver)
        executor.execute("driver.goForward()", driver)

        coVerify { driver.scrollDown(1) }
        coVerify { driver.scrollUp(3) }
        coVerify { driver.scrollToTop() }
        coVerify { driver.scrollToBottom() }
        coVerify { driver.scrollToMiddle(0.7) }
        coVerify { driver.scrollToScreen(0.5) }
        coVerify { driver.scrollToScreen(1.25) }
        coVerify { driver.goBack() }
        coVerify { driver.goForward() }
    }

    @Test
    fun `error handling returns null`() = runBlocking {
        val driver = mockk<WebDriver>()
        coEvery { driver.click(any(), any()) } throws IllegalStateException("boom")

        val r = executor.execute("driver.click(\"#a\")", driver)
        assertNull(r)
    }

    @Test
    fun `toolCall to expression end-to-end few samples`() = runBlocking {
        val driver = mockk<WebDriver>(relaxed = true)
        // exists
        val tc1 = ToolCall("driver", "exists", mapOf("selector" to "#a"))
        executor.execute(tc1, driver)
        coVerify { driver.exists("#a") }
        // clickMatches
        val tc2 = ToolCall("driver", "clickMatches", mapOf(
            "selector" to "a",
            "attrName" to "href",
            "pattern" to "foo",
            "count" to 2
        ))
        executor.execute(tc2, driver)
        coVerify { driver.clickMatches("a", "href", "foo", 2) }
        // waitForNavigation
        val tc3 = ToolCall("driver", "waitForNavigation", mapOf("oldUrl" to "u", "timeoutMillis" to 10))
        coEvery { driver.waitForNavigation("u", 10) } returns 5L
        val r = executor.execute(tc3, driver)
        assertEquals(5L, r)
    }
}

