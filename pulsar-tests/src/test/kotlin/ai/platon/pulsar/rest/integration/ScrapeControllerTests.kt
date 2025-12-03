package ai.platon.pulsar.rest.integration

import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.Ignore
import kotlin.test.assertTrue

@Ignore("Websites might be fall, run these integration tests manually")
@Tag("TimeConsumingTest")
class ScrapeControllerTests : IntegrationTestBase() {

    protected val originUrl = "https://www.amazon.com/"
    protected val productUrl = "https://www.amazon.com/dp/B08PP5MSVB"

    fun testScraping(url: String) {
        val result = scrape(url)
        assertTrue(result != null, "Result should not be null | $url")
        println(pulsarObjectMapper().writeValueAsString(result))
    }

    fun testLLMScraping(url: String) {
        val result = llmScrape(url)?.resultSet
        assertTrue(result != null, "Result should not be null | $url")
        println(result)
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
