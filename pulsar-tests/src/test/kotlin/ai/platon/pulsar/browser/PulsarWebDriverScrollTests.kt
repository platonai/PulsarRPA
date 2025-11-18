package ai.platon.pulsar.browser

import ai.platon.pulsar.WebDriverTestBase
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.joinAll
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PulsarWebDriverScrollTests : WebDriverTestBase() {

    override val webDriverService get() = FastWebDriverService(browserFactory)

    @Test
    fun `test scrollBy`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val scrollY = driver.scrollBy(200.0, smooth = true)

        assertEquals(200.0, scrollY, 1.0)
        assertEquals(200.0, driver.evaluate("window.scrollY", 200.0), 1.0)
    }

    // Helpers & data structures for extended scroll tests
    data class ScrollMetrics(
        val scrollY: Double,
        val viewportHeight: Double,
        val totalHeight: Double,
        val maxScrollY: Double
    )

    companion object {
        private const val SMALL_TOL = 1.0
        private const val MEDIUM_TOL = 3.0
        private const val LARGE_TOL = 5.0
    }

    private suspend fun getScrollMetrics(driver: WebDriver): ScrollMetrics {
        val scrollY = driver.evaluate("window.scrollY", 0.0)
        val viewportHeight = driver.evaluate("window.innerHeight", 0.0)
        val totalHeight = driver.evaluate(
            "Math.max(document.documentElement.scrollHeight, document.body.scrollHeight)",
            0.0
        )
        val maxScrollY = (totalHeight - viewportHeight).let { if (it < 0) 0.0 else it }
        return ScrollMetrics(scrollY, viewportHeight, totalHeight, maxScrollY)
    }

    private suspend fun ensureTallPage(driver: WebDriver, minHeight: Double) {
        val totalHeight = driver.evaluate(
            "Math.max(document.documentElement.scrollHeight, document.body.scrollHeight)",
            0.0
        )
        if (totalHeight >= minHeight) return
        driver.evaluate(
            """(function(minH){
              const body = document.body;
              let h = Math.max(document.documentElement.scrollHeight, body.scrollHeight);
              let i = 0;
              while(h < minH && i < 300){
                const d = document.createElement('div');
                d.style.height='250px';
                d.style.border='1px solid #eee';
                d.textContent='pad-' + i;
                body.appendChild(d);
                h = Math.max(document.documentElement.scrollHeight, body.scrollHeight);
                i++;
              }
              return h;
            })(${minHeight})""".trimIndent(),
            0.0
        )
    }

    private fun assertAlmostEquals(expected: Double, actual: Double, tol: Double, msg: String? = null) {
        assertTrue(abs(expected - actual) <= tol, msg ?: "Expected ~$expected ±$tol, actual=$actual")
    }

    // Extended test cases for scrollBy covering edge conditions and behavior nuances

    @Test
    fun `scrollBy zero delta at top is idempotent`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val m0 = getScrollMetrics(driver)
        assertEquals(0.0, m0.scrollY, SMALL_TOL)
        val y = driver.scrollBy(0.0, smooth = true)
        val m1 = getScrollMetrics(driver)
        assertAlmostEquals(0.0, y, SMALL_TOL)
        assertAlmostEquals(m0.scrollY, m1.scrollY, SMALL_TOL)
        assertEquals(driver.evaluate("window.scrollX", 0.0), 0.0, SMALL_TOL)
    }

    @Test
    fun `scrollBy small positive single step`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val m0 = getScrollMetrics(driver)
        assertEquals(0.0, m0.scrollY, MEDIUM_TOL)
        val delta = 5.0
        val y = driver.scrollBy(delta, smooth = true)
        val m1 = getScrollMetrics(driver)
        assertAlmostEquals(delta, y, MEDIUM_TOL)
        assertAlmostEquals(delta, m1.scrollY, MEDIUM_TOL)
    }

    @Test
    fun `scrollBy negative at top clamps to zero`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        val m0 = getScrollMetrics(driver)
        assertEquals(0.0, m0.scrollY, SMALL_TOL)
        val y = driver.scrollBy(-200.0, smooth = true)
        assertAlmostEquals(0.0, y, SMALL_TOL)
        val m1 = getScrollMetrics(driver)
        assertAlmostEquals(0.0, m1.scrollY, SMALL_TOL)
    }

    @Test
    fun `scrollBy negative mid page decreases position`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        ensureTallPage(driver, 3000.0)
        driver.scrollBy(800.0, smooth = true)
        val mBefore = getScrollMetrics(driver)
        val y2 = driver.scrollBy(-400.0, smooth = true)
        val mAfter = getScrollMetrics(driver)
        assertTrue(mAfter.scrollY < mBefore.scrollY + SMALL_TOL)
        assertAlmostEquals(mBefore.scrollY - 400.0, y2, LARGE_TOL)
    }

    @Test
    fun `scrollBy overscroll bottom clamps`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        ensureTallPage(driver, 4000.0)
        val y = driver.scrollBy(20000.0, smooth = true)
        val m = getScrollMetrics(driver)
        assertAlmostEquals(m.maxScrollY, y, LARGE_TOL)
        assertAlmostEquals(m.maxScrollY, m.scrollY, LARGE_TOL)
    }

    @Test
    fun `scrollBy overscroll upward from bottom clamps to zero`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        ensureTallPage(driver, 4000.0)
        driver.scrollBy(20000.0, smooth = true)
        val y = driver.scrollBy(-20000.0, smooth = true)
        assertAlmostEquals(0.0, y, SMALL_TOL)
        val m = getScrollMetrics(driver)
        assertAlmostEquals(0.0, m.scrollY, SMALL_TOL)
    }

    @Test
    fun `scrollBy smooth vs instant produce similar final position`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        ensureTallPage(driver, 3000.0)
        val ySmooth = driver.scrollBy(600.0, smooth = true)
        val yInstant = driver.scrollBy(600.0, smooth = false)
        assertTrue(abs(yInstant - ySmooth) <= LARGE_TOL, "Difference ${abs(yInstant - ySmooth)} too large")
    }

    @Test
    fun `scrollBy sequential cumulative small deltas`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        ensureTallPage(driver, 3000.0)
        var expected = 0.0
        repeat(5) {
            expected += 120.0
            val y = driver.scrollBy(120.0, smooth = true)
            assertAlmostEquals(expected, y, LARGE_TOL)
        }
        val m = getScrollMetrics(driver)
        assertAlmostEquals(expected, m.scrollY, LARGE_TOL)
    }

    @Test
    fun `scrollBy clamp boundary equality near bottom`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        ensureTallPage(driver, 5000.0)
        val m0 = getScrollMetrics(driver)
        val targetDelta = m0.maxScrollY * 0.9
        driver.scrollBy(targetDelta, smooth = true)
        val mMid = getScrollMetrics(driver)
        val y2 = driver.scrollBy(mMid.maxScrollY * 0.5, smooth = true) // should clamp
        val m2 = getScrollMetrics(driver)
        assertAlmostEquals(m2.maxScrollY, y2, LARGE_TOL)
        assertAlmostEquals(m2.maxScrollY, m2.scrollY, LARGE_TOL)
    }

    @Test
    fun `scrollBy negative single pixel`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        ensureTallPage(driver, 3000.0)
        driver.scrollBy(300.0, smooth = true)
        val before = getScrollMetrics(driver)
        val y = driver.scrollBy(-1.0, smooth = true)
        val after = getScrollMetrics(driver)
        assertTrue(after.scrollY <= before.scrollY + SMALL_TOL)
        assertAlmostEquals(before.scrollY - 1.0, y, MEDIUM_TOL)
    }

    @Test
    fun `scrollBy rapid smooth sequence stability`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        ensureTallPage(driver, 3000.0)
        val deltas = listOf(50.0, 75.0, 100.0, -30.0, 60.0)
        var expected = 0.0
        deltas.forEach { d ->
            expected += d
            expected = expected.coerceIn(0.0, getScrollMetrics(driver).maxScrollY)
            val y = driver.scrollBy(d, smooth = true)
            assertAlmostEquals(expected, y, LARGE_TOL)
        }
        val m = getScrollMetrics(driver)
        assertAlmostEquals(expected, m.scrollY, LARGE_TOL)
    }

    @Test
    fun `scrollBy return value consistency`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        ensureTallPage(driver, 3000.0)
        val y = driver.scrollBy(450.0, smooth = true)
        val reported = driver.evaluate("window.scrollY", 0.0)
        assertAlmostEquals(reported, y, MEDIUM_TOL)
    }

    @Test
    fun `scrollBy concurrent smooth calls serialized outcome`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        ensureTallPage(driver, 3000.0)
        coroutineScope {
            val jobs = listOf(150.0, 200.0, 250.0).map { d ->
                launch { driver.scrollBy(d, smooth = true) }
            }
            jobs.joinAll()
        }
        val m = getScrollMetrics(driver)
        val expected = 150.0 + 200.0 + 250.0
        assertAlmostEquals(expected, m.scrollY, LARGE_TOL)
    }

    @Test
    fun `scrollBy on short page has no effect`() = runEnhancedWebDriverTest(multiScreensInteractiveUrl, browser) { driver ->
        // Shrink page content to be shorter than viewport
        driver.evaluate("document.body.innerHTML='<div style=\"height:50px\">Short</div>'", 0.0)
        val m0 = getScrollMetrics(driver)
        assertEquals(0.0, m0.maxScrollY, SMALL_TOL)
        val y1 = driver.scrollBy(500.0, smooth = true)
        val m1 = getScrollMetrics(driver)
        assertAlmostEquals(0.0, y1, SMALL_TOL)
        assertAlmostEquals(0.0, m1.scrollY, SMALL_TOL)
    }
}
