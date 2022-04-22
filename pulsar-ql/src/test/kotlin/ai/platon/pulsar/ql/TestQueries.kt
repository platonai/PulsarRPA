package ai.platon.pulsar.ql

import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.ql.h2.Queries
import org.junit.Test
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Created by vincent on 17-7-29.
 * Copyright @ 2013-2017 Platon AI. All rights reserved.
 */
class TestQueries: TestBase() {

    private val portalUrl = "https://www.amazon.com/Best-Sellers/zgbs"
    private val args = "-i 1s -ii 5s -ol a[href~=/dp/] -ignoreFailure"
    private val url = "$portalUrl $args"
    private val restrictCss = "a[href~=/dp/]"

    @Test
    fun testLoadOutPages() {
//        val portalUrl = "http://gxt.jl.gov.cn/xxgk/zcwj/"
//        val restrictCss = "#content ul li a"

        val limit = 20
        val pages = Queries.loadOutPages(session, url, restrictCss, 1, limit)
        pages.map { it.url }.distinct().forEachIndexed { i, url -> println("$i.\t$url") }
        assertTrue("Page size: " + pages.size) { pages.size <= limit }
    }

    @Test
    fun testLoadOutPagesInParallel() {
        val parallel = 5
        val limit = 10

        val executor = Executors.newWorkStealingPool()
        val futures = IntRange(1, parallel).map {
            executor.submit<Collection<WebPage>> {
                val pages = Queries.loadOutPages(session, url, restrictCss, 1, limit)
                pages.map { it.url }.distinct().forEachIndexed { i, url -> println("$i.\t$url") }
                assertTrue("Page size: " + pages.size) { pages.size <= limit }
                pages
            }
        }
        val pageLists = futures.map { it.get() }

        assertEquals(pageLists.size, parallel)
        pageLists.forEach {
            assertTrue { it.size <= limit }
        }
    }
}
