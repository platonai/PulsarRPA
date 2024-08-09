package ai.platon.pulsar.test.session

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.skeleton.common.persist.ext.options
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.dom.FeaturedDocument
import ai.platon.pulsar.persist.model.WebPageFormatter
import ai.platon.pulsar.test.TestBase
import com.google.gson.Gson
import java.nio.file.Files
import kotlin.test.*
import java.time.Instant
import java.util.*

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class PulsarSessionTests: TestBase() {
    private val timestamp = System.currentTimeMillis()
    private val url = "https://www.amazon.com/Best-Sellers/zgbs?t=$timestamp"
    private val url2 = "https://www.amazon.com/Best-Sellers-Beauty/zgbs/beauty?t=$timestamp"
    
    private val resourceUrl = "https://www.amazon.com/robots.txt?t=$timestamp"

    @BeforeTest
    fun setup() {
        // The data store is FileStore, and delete does not work
//        webDB.delete(url)
//        webDB.delete(url2)
    }

    @Test
    fun testNormalize() {
        val normURL = session.normalize(url)
        assertNotEquals(session.sessionConfig, normURL.options.conf)
        val page = session.load(normURL)
        assertEquals(normURL.options.conf, page.conf)
    }

    @Test
    fun testLoad() {
        val page = session.load(url)
        val page2 = webDB.getOrNull(url)

        if (page.protocolStatus.isSuccess) {
            assertNotNull(page2)
            assertTrue { page2.fetchCount > 0 }
            assertTrue { page2.protocolStatus.isSuccess }
        }

        if (page2 != null) {
            println(WebPageFormatter(page2))
            println(page2.vividLinks)
            val gson = Gson()
            println(gson.toJson(page2.activeDOMStatus))
            println(gson.toJson(page2.activeDOMStatTrace))
        }
    }

    @Test
    fun testLoadResource() {
        val page = session.loadResource(resourceUrl, url, "-refresh")

        assertTrue { page.fetchCount > 0 }
        assertTrue { page.protocolStatus.isSuccess }

        println(WebPageFormatter(page))
        val path = session.export(page)
        println("Webpage exported | $path")
    }

    @Test
    fun testLoadLocalFile() {
        val path = AppPaths.getTmp("test.html")
        println(path)
        val html = """
            <html>
            <head>
            <title>Test</title>
            </head>
            <body>
            <h1>Hello</h1>
            <a href="http://www.example.com">Example</a>
            </body>
            </html>
        """.trimIndent()
        Files.writeString(path, html)
        val base64 = Base64.getUrlEncoder().encode(path.toString().toByteArray()).toString(Charsets.UTF_8)
        val url = "http://localfile.org?path=$base64"
        val document = session.loadDocument(url, "-refresh")
        assertEquals("Hello", document.selectFirstTextOrNull("h1"))
    }

    @Test
    fun testFetchForExpires() {
        val seconds = 5L
        val args = "-i ${seconds}s"
        var startTime = Instant.now()
        println("Start time: $startTime")

        val page = session.load(url2, args = args)
        val prevFetchTime1 = page.prevFetchTime
        val fetchTime1 = page.fetchTime
        val fetchCount1 = page.fetchCount

        assertTrue { page.protocolStatus.isSuccess }
        assertTrue { page.isFetched }
        assertTrue { page.isContentUpdated }
        assertTrue("prevFetchTime: $prevFetchTime1 startTime: $startTime") { startTime < prevFetchTime1 }
        assertTrue("prevFetchTime: $prevFetchTime1 startTime: $startTime") { prevFetchTime1 < fetchTime1 }
        assertEquals(seconds, page.fetchInterval.seconds)
        assertTrue { page.fetchTime > startTime }
        assertEquals(page.prevFetchTime.plusSeconds(seconds), page.fetchTime)
        assertTrue { page.fetchTime > page.prevFetchTime }
//        assertTrue { page.fetchCount > 1 }
        assertEquals(1, page.fetchCount)

        sleepSeconds(6)
        startTime = Instant.now()
        val page2 = session.load(url, args = args)
        val prevFetchTime2 = page2.prevFetchTime
        val fetchTime2 = page2.fetchTime
        val fetchCount2 = page2.fetchCount

        assertTrue { page.protocolStatus.isSuccess }
        assertTrue { page2.isFetched }
        assertTrue { page2.isContentUpdated }
        assertTrue { prevFetchTime2 > startTime }
        assertTrue { prevFetchTime2 > prevFetchTime1 }
        assertTrue { fetchTime2 > startTime }
        assertTrue { fetchTime2 > page2.prevFetchTime }
        
        assertEquals(fetchCount1 + 1, fetchCount2)
    }

    @Test
    fun testFetchForExpireAt() {
        val now = Instant.now()
        val seconds = 5L
        val args = "-i ${seconds}s"
        var options = session.options(args)
        var startTime = Instant.now()
        println("Start time: $startTime")

        val page = session.load(url2, options)
        val prevFetchTime1 = page.prevFetchTime
        val fetchTime1 = page.fetchTime
        val fetchCount1 = page.fetchCount

        println("Round 1 checking ....")
        assertTrue("${page.protocolStatus}") { page.protocolStatus.isSuccess }
        assertTrue("Should be fetched for random url") { page.isFetched }
        assertTrue("Content should be updated for random url") { page.isContentUpdated }
        assertTrue { page.options.expires.seconds > 0 }
        assertTrue("Not expired, prevFetchTime should be before startTime: $prevFetchTime1 -> $startTime") {
            startTime > prevFetchTime1
        }
        assertTrue("prevFetchTime: $prevFetchTime1 fetchTime1: $fetchTime1") {
            prevFetchTime1 < fetchTime1
        }
        assertTrue { page.fetchTime > startTime }
        // since expireAt is used
//        assertEquals(prevFetchTime1.plusSeconds(seconds), page.fetchTime)
        assertTrue { page.fetchTime > page.prevFetchTime }
        assertTrue { page.fetchCount > 1 }
        options = session.options().apply { expireAt = now.plusSeconds(seconds) }

        println("Wait for 6 seconds so the page is expired .... | $options")
        sleepSeconds(6)
        startTime = Instant.now()
        assertTrue("expireAt: ${options.expireAt} startTime: $startTime") {
            options.expireAt < startTime
        }
        assertTrue("Should be expired at ${options.expireAt} <- $now") {
            options.isExpired(page.prevFetchTime)
        }
        val page2 = session.load(url, options)
        val prevFetchTime2 = page2.prevFetchTime
        val fetchTime2 = page2.fetchTime
        val fetchCount2 = page2.fetchCount

        println("Round 2 checking ....")
        assertTrue { page.protocolStatus.isSuccess }
        assertTrue { page2.isFetched }
        assertTrue { page2.isContentUpdated }
        assertTrue { prevFetchTime2 > startTime }
        assertTrue("prevFetchTime1: $prevFetchTime1 prevFetchTime2: $prevFetchTime2") {
            prevFetchTime2 > prevFetchTime1
        }
        assertTrue { fetchTime2 > startTime }
        assertTrue { fetchTime2 > prevFetchTime2 }
        assertEquals(fetchCount1 + 1, fetchCount2)
    }
}
