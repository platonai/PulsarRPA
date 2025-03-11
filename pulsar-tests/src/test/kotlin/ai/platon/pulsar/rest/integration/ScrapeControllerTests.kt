package ai.platon.pulsar.rest.integration

import ai.platon.pulsar.common.serialize.json.pulsarObjectMapper
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.Ignore

@Ignore("Websites might be fall, run these integration tests manually")
@Tag("TimeConsumingTest")
class ScrapeControllerTests : IntegrationTestBase() {

    protected val originUrl = "https://www.amazon.com/"
    protected val productUrl = "https://www.amazon.com/dp/B0C1H26C46"

    fun testScraping(url: String) {
        val result = scrape(url)
        require(result != null) { "Result should not be null | $url" }
        println(pulsarObjectMapper().writeValueAsString(result))
    }

    @Test
    fun testScraping() {
        testScraping("https://www.amazon.com")
        testScraping("https://www.amazon.com/dp/B0C1H26C46")

        testScraping("https://www.jd.com/")
        testScraping("https://www.ebay.com/")
    }
}
