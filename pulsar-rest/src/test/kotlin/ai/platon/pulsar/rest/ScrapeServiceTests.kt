package ai.platon.pulsar.rest

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.boot.autoconfigure.pulsar.test.PulsarTestContextInitializer
import ai.platon.pulsar.common.ResourceStatus
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.crawl.StreamingCrawlLoop
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.dom.nodes.node.ext.cleanText
import ai.platon.pulsar.persist.jackson.pulsarObjectMapper
import ai.platon.pulsar.rest.api.entities.ScrapeRequest
import ai.platon.pulsar.rest.api.entities.ScrapeStatusRequest
import ai.platon.pulsar.rest.api.service.ScrapeService
import org.apache.commons.lang3.RandomStringUtils
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(SpringRunner::class)
@SpringBootTest
@ContextConfiguration(initializers = [PulsarTestContextInitializer::class])
class ScrapeServiceTests {
    final val username = RandomStringUtils.randomAlphanumeric(8)

    @Autowired
    private lateinit var service: ScrapeService

    @Autowired
    private lateinit var session: PulsarSession

    @Autowired
    private lateinit var globalCache: GlobalCache

    private val crawlLoop by lazy { StreamingCrawlLoop(session, globalCache) }

    @Before
    fun setup() {
        crawlLoop.start()
    }

    @After
    fun tearDown() {
        crawlLoop.stop()
    }

    /**
     * Execute a normal sql
     * */
    @Test
    fun `Sum of 1+1 is 2`() {
        val sql = "select 1+1 as sum"
        val request = ScrapeRequest(sql)

        val response = service.executeQuery(request)
        val records = response.resultSet
        assertNotNull(records)

        assertTrue { records.isNotEmpty() }
        assertEquals(2, records[0]["sum"].toString().toInt())
    }

    @Test
    fun `When scrape jd using load_and_select then the result returns synchronously`() {
        val url = "https://www.jd.com/"
        val redirectedUrl = "https://global.jd.com/"
        val sql = "select dom_base_uri(dom) as uri from load_and_select('$url -i 0s', ':root')"
        val request = ScrapeRequest(sql)

        val response = service.executeQuery(request)
        val records = response.resultSet
        assertNotNull(records)

        assertTrue { records.isNotEmpty() }
        val actualUrl = records[0]["uri"].toString()
        assertTrue { actualUrl == url || actualUrl == redirectedUrl }
    }

    @Test
    fun `When call amazon_suggestions then the suggestions are retrieved`() {
        val url = "https://www.amazon.com/"
        val sql = "select amazon_suggestions('$url -i 0s', 'cups') as suggestions"
        val request = ScrapeRequest(sql)

        val response = service.executeQuery(request)
        val records = response.resultSet
        println(pulsarObjectMapper().writeValueAsString(response))

        assertEquals(200, response.pageStatusCode, response.pageStatus)
        assertEquals(200, response.statusCode, response.status)
        assertNotNull(records)

        assertTrue { records.isNotEmpty() }
    }

    @Test
    fun `When extract result abstract then it is retrieved`() {
        val url = "https://www.amazon.com/s?rh=n:3396311&rd=1&fs=true"

        val text = session.loadDocument("$url -i 1s").selectFirst("h1").cleanText
        assertTrue(text.isNotBlank())
    }

    @Test
    fun `When extract pagination then it is retrieved`() {
        val url = "https://www.amazon.com/s?rh=n:3396311&rd=1&fs=true"

        val sql = "select\n" +
                "    dom_base_uri(dom) as `url`,\n" +
                "    dom_first_text(dom, 'h1 div span:containsOwn(results for), h1 div span:containsOwn(results)') as `results`,\n" +
                "    array_join_to_string(dom_all_texts(dom, 'ul.a-pagination > li, div#pagn > span'), '|') as `pagination`\n" +
                "from load_and_select('$url -i 1s', 'body');"

        val request = ScrapeRequest(sql)

        val response = service.executeQuery(request)
        println(pulsarObjectMapper().writeValueAsString(response))
        val records = response.resultSet
        assertNotNull(records)

        assertTrue { records.isNotEmpty() }
        println(records)
        val record = records[0]
        assertEquals(url, record["url"].toString())
        // TODO: failed if the language is Chinese
        // assertTrue { record["results"].toString().contains("results") }
        val pagination = record["pagination"].toString()
        assertTrue { "Next" in pagination || "下一页" in pagination }
    }

    @Test
    fun `When scrape jd then the base uri returns asynchronously`() {
        val url = "https://www.jd.com/ -i 0s"
        val sql = "select dom_base_uri(dom) as uri from load_and_select('$url', ':root')"
        val request = ScrapeRequest(sql)

        val uuid = service.submitJob(request)

        assertTrue { uuid.isNotEmpty() }
        println(uuid)

        val scrapeStatusRequest = ScrapeStatusRequest(uuid)
        var status = service.getStatus(scrapeStatusRequest)
        var i = 60
        while (i-- > 0 && status.statusCode != ResourceStatus.SC_OK) {
            sleepSeconds(1)
            status = service.getStatus(scrapeStatusRequest)
        }
        println(pulsarObjectMapper().writeValueAsString(status))
        assertTrue { i > 0 }
        assertEquals(200, status.statusCode)
    }
}
