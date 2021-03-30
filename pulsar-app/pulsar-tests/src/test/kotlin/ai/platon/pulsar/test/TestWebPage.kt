package ai.platon.pulsar.test

import ai.platon.pulsar.common.PulsarParams
import ai.platon.pulsar.common.message.CompletedPageFormatter
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.persist.metadata.Name
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class TestWebPage: TestBase() {
    val url = "https://www.amazon.com/dp/B082P8J28M?t=" + System.currentTimeMillis()

    @Test
    fun testFetchTime() {
        val args = "-i 5s"
        val normalizedArgs = "-expires PT5S"
        val option = session.options(args)
        var page = session.load(url, option)
        val prevFetchTime1 = page.prevFetchTime
        val fetchTime1 = page.fetchTime

        println("Fetch time history: " + page.getFetchTimeHistory(""))

        assertTrue { page.protocolStatus.isSuccess }
        assertTrue { page.isContentUpdated }
        assertEquals(option, page.variables[PulsarParams.VAR_LOAD_OPTIONS])
        assertEquals(normalizedArgs, page.args)

        sleepSeconds(10)

        page = session.load(url, args)
        assertTrue { page.protocolStatus.isSuccess }
        assertTrue { page.isContentUpdated }
        val prevFetchTime2 = page.prevFetchTime
        val fetchTime2 = page.fetchTime

        println(CompletedPageFormatter(page, "", true, true, true, true))
        println("Fetch time history: " + page.getFetchTimeHistory(""))
        println("prevFetchTime: " + page.prevFetchTime)
        println("fetchTime: " + page.fetchTime)
        println("lastFetchTime: " + page.lastFetchTime)
        val responseTime = page.metadata[Name.RESPONSE_TIME]?:""
        println(responseTime)
        println(Instant.now())
        println("fetchCount: " + page.fetchCount)
        println("fetchInterval: " + page.fetchInterval)

        assertTrue { prevFetchTime1 < prevFetchTime2 }
        assertEquals(prevFetchTime2, page.fetchTime)
        assertTrue { fetchTime1 < page.fetchTime }
        assertEquals(2, page.fetchCount)
    }
}
