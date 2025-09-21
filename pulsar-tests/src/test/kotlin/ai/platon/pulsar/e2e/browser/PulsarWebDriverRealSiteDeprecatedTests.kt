package ai.platon.pulsar.e2e.browser

import org.junit.jupiter.api.Tag
import kotlin.test.*

@Ignore("Tests deprecated, they are not reliable")
@Tag("TimeConsumingTest")
class PulsarWebDriverRealSiteDeprecatedTests : PulsarWebDriverRealSiteTests() {

    @Test
    fun `open a HTML page and compute screen number`() = runWebDriverTest(originUrl, browser) { driver ->
        driver.evaluate("__pulsar_utils__.scrollToTop()")
        var metadata = computeActiveDOMMetadata(driver)
        assertEquals(0f, metadata.screenNumber)

        driver.evaluate("window.scrollTo(0, 300)")
        metadata = computeActiveDOMMetadata(driver)
        assertTrue { metadata.screenNumber > 0.0 }
        assertTrue { metadata.screenNumber < 1.0 }

        driver.evaluate("window.scrollTo(0, 1080)")
        metadata = computeActiveDOMMetadata(driver)
        assertTrue { metadata.screenNumber > 1 }
    }

    @Test
    fun test_selectAttributes() = runWebDriverTest(browser) { driver ->
        driver.navigateTo(productUrl)

        val selector = "input[type=text]"
        driver.waitForSelector(selector)

        val attributes = driver.selectAttributes(selector)
        assertTrue { attributes.isNotEmpty() }
        println("attributes: $attributes")
    }

}
