package ai.platon.pulsar.rest


import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

/**
 * Created by vincent on 17-8-1.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 */
class TestPageResource : KResourceTestBase() {
    @Test
    fun testLoadOutPages() {
        val url = "http://news.cjn.cn/gnxw/"
        val args = "-ps,-rpl,-prst,--expires=10m,-amin=8,-amax=1150,-umin=30,-umax=200"
        val args2 = "-ps,-rpl,-prst,--expires=1m"
        val start = 1
        val limit = 20
        val log = 1

        var result: MutableMap<String, Any> = pageResourceReference.loadOutPages(url, args, args2, start, limit, log)
        val totalCount = (result["totalCount"] as Double).toInt()
        val docs = result["docs"] as ArrayList<Map<String, Any>>
        val debug = result["debug"] as Map<String, Any>

        LOG.debug(debug.toString())

        result.remove("docs")
        result.remove("debug")
        assertTrue(!result.isEmpty())
        assertTrue(totalCount > 0)

        LOG.debug(result.toString())

        LOG.debug("The main page: ")
        result = pageResourceReference.load(url)
        LOG.debug(result.toString())
    }
}
