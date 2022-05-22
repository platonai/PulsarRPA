package ai.platon.pulsar.crawl.common.collect

import ai.platon.pulsar.common.PulsarParams
import ai.platon.pulsar.common.message.LoadStatusFormatter
import ai.platon.pulsar.common.persist.ext.options
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.persist.metadata.Name
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class WebPageTests {
    private val session = PulsarContexts.createSession()
    val url = "https://www.amazon.com/dp/B082P8J28M"

    @Before
    fun setup() {
        session.context.webDb.delete(url)
    }

    @Test
    fun testFetchTime() {
        // BrowserSettings.withBrowser(BrowserType.PLAYWRIGHT_CHROME.name)

        val args = "-i 5s -njr 3"
        val normalizedArgs = "-expires PT5S -nJitRetry 3"
        val option = session.options(args)

        // expired, so the page should be fetched
        var page = session.load(url, option)
        val prevFetchTime1 = page.prevFetchTime
        val fetchTime1 = page.fetchTime

        println("Fetch time history: " + page.getFetchTimeHistory(""))

        assertTrue { page.protocolStatus.isSuccess }
        assertTrue { page.isContentUpdated }
        assertEquals(option, page.variables[PulsarParams.VAR_LOAD_OPTIONS])
        assertEquals(normalizedArgs, page.args)

        sleepSeconds(5)
        val expireAt = Instant.now()
        sleepSeconds(5)

        val options2 = session.options("$args -expireAt $expireAt")
        assertTrue { options2.isExpired(page.prevFetchTime) }

        // expired, so fetch the page
        page = session.load(url, options2)
        assertTrue { page.protocolStatus.isSuccess }
        assertTrue { page.isContentUpdated }
        assertEquals(options2, page.options)
        val prevFetchTime2 = page.prevFetchTime
        val fetchTime2 = page.fetchTime

        println(LoadStatusFormatter(page, "", true, true, true, true))
        println("Fetch time history: " + page.getFetchTimeHistory(""))
        println("prevFetchTime: " + page.prevFetchTime)
        // fetch time is updated
        println("fetchTime: " + page.fetchTime)
        val responseTime = page.metadata[Name.RESPONSE_TIME]?:""
        println(responseTime)
        println(Instant.now())
        println("fetchCount: " + page.fetchCount)
        println("fetchInterval: " + page.fetchInterval)

        assertEquals(page.options.fetchInterval, page.fetchInterval)
        assertTrue { prevFetchTime1 < prevFetchTime2 }
        assertEquals(0L, Duration.between(page.fetchTime, Instant.now() + page.options.fetchInterval).seconds)
        assertTrue { fetchTime1 < page.fetchTime }

        // TODO: test failed when FileBackendPageStore is used
        // assertEquals(2, page.fetchCount)
    }
}
