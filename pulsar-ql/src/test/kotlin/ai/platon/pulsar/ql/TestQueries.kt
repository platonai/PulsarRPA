package ai.platon.pulsar.ql

import ai.platon.pulsar.ql.h2.Queries
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Created by vincent on 17-7-29.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class TestQueries: TestBase() {

    @Test
    fun testLoadOutPages() {
//        val portalUrl = "https://www.amazon.com/Best-Sellers/zgbs"
//        val restrictCss = "a[href~=/dp/]"
        val portalUrl = "http://gxt.jl.gov.cn/xxgk/zcwj/"
        val restrictCss = "#content ul li a"

        val limit = 30
        val pages = Queries.loadOutPages(session, portalUrl, restrictCss, 1, limit)
        pages.map { it.url }.distinct().forEachIndexed { i, url -> println("$i.\t$url") }
        assertTrue("Page size: " + pages.size) { pages.size <= limit }
    }
}
