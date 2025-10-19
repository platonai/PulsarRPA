package ai.platon.pulsar.e2e

import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import ai.platon.pulsar.common.logPrintln
import ai.platon.pulsar.integration.rest.IntegrationTestBase
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.Ignore
import kotlin.test.assertTrue

@Ignore("Websites might be fall, run these integration tests manually")
@Tag("E2ETest")
class ScrapeControllerE2ETest : IntegrationTestBase() {

    fun testScraping(url: String) {
        val result = scrape(url)
        assertTrue(result != null, "Result should not be null | $url")
        logPrintln(pulsarObjectMapper().writeValueAsString(result))
    }

    fun testLLMScraping(url: String) {
        val result = llmScrape(url)?.resultSet
        assertTrue(result != null, "Result should not be null | $url")
        logPrintln(result)
    }

    @Test
    fun testScraping() {
        testScraping("https://www.amazon.com")
        testScraping("https://www.amazon.com/dp/B08PP5MSVB")

        testScraping("https://www.jd.com/")
        testScraping("https://www.ebay.com/")
    }

    @Test
    fun testLLMScraping() {
        testLLMScraping("https://www.amazon.com")
        testLLMScraping("https://www.amazon.com/dp/B08PP5MSVB")
    }
}

