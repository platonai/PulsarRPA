package ai.platon.pulsar.crawl.parse

import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.common.urls.UrlUtils
import ai.platon.pulsar.common.urls.UrlUtils.reverseUrl
import ai.platon.pulsar.crawl.filter.CrawlFilters
import kotlin.test.*
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringRunner
import java.io.IOException
import java.net.MalformedURLException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@Ignore
@RunWith(SpringRunner::class)
@ContextConfiguration(locations = ["classpath:/test-context/filter-beans.xml"])
class TestCrawlFilter {
    private val detailUrls = arrayOf(
            "http://mall.jumei.com/product_200918.html?from=store_lancome_list_items_7_4"
    )

    @Autowired
    lateinit var conf: MutableConfig
    @Autowired
    lateinit var crawlFilters: CrawlFilters

    @BeforeTest
    @Throws(IOException::class)
    fun setUp() {
        val crawlFilterRules = "{}"
        conf[CrawlFilters.CRAWL_FILTER_RULES] = crawlFilterRules
    }

    @Test
    @Throws(MalformedURLException::class)
    fun testKeyRange() {
        val keyRange = crawlFilters.reversedKeyRanges
        println(keyRange)
        assertEquals("com.jumei.mall:http/\uFFFF", keyRange["com.jumei.mall:http"])
        assertEquals("com.jumei.lancome:http/search.html\uFFFF", keyRange["com.jumei.lancome:http/search.html"])
        assertNotEquals("com.jumei.lancome:http/search.html\\uFFFF", keyRange["com.jumei.lancome:http/search.html"])
        for (detailUrl in detailUrls) {
            assertTrue(crawlFilters.testKeyRangeSatisfied(reverseUrl(detailUrl)))
        }
    }

    @Test
    @Throws(MalformedURLException::class)
    fun testMaxKeyRange() {
        val keyRange = arrayOf("\u0000", "\uFFFF")
        println(keyRange[0].toString() + ", " + keyRange[1])
        assertEquals(65438, '\uFFFF' - 'a')
        for (url in detailUrls) {
            val reversedUrl = UrlUtils.reverseUrl(url)
            assertTrue("com.jumei.lancome:http/search.html" < reversedUrl)
            assertTrue("com.jumei.mall:http/\uFFFF" > reversedUrl)
            // Note : \uFFFF, not \\uFFFF
            assertFalse("com.jumei.mall:http/\\uFFFF" > reversedUrl)
        }

        for (url in detailUrls) {
            val reversedUrl = UrlUtils.reverseUrl(url)
            println(reversedUrl)
            assertTrue(keyRange[0] < reversedUrl)
            assertTrue(keyRange[1] > reversedUrl)
        }
    }
}
