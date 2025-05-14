package ai.platon.pulsar.browser

import ai.platon.pulsar.browser.common.SimpleScriptConfuser.Companion.IDENTITY_NAME_MANGLER
import ai.platon.pulsar.common.*
import ai.platon.pulsar.common.emoji.PopularEmoji
import ai.platon.pulsar.common.proxy.ProxyEntry
import ai.platon.pulsar.common.serialize.json.prettyPulsarObjectMapper
import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.persist.model.ActiveDOMMessage
import ai.platon.pulsar.persist.model.ActiveDOMMetadata
import ai.platon.pulsar.skeleton.crawl.fetch.driver.AbstractWebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.driver.WebDriver
import ai.platon.pulsar.skeleton.crawl.fetch.privacy.BrowserId
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Tag
import java.io.IOException
import java.net.Proxy
import java.nio.file.Path
import java.text.MessageFormat
import java.time.Duration
import java.util.*
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
