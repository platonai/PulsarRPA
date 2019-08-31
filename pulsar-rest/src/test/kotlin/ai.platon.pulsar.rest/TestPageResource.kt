package ai.platon.pulsar.rest

import ai.platon.pulsar.common.config.PulsarConstants.DEFAULT_PULSAR_MASTER_PORT
import ai.platon.pulsar.rest.rpc.PageResourceReference
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import kotlin.test.assertTrue

open class TestPageResource {
    private val log = LoggerFactory.getLogger(TestPageResource::class.java)
    private val seedUrl = "http://news.cqnews.net/html/2017-06/08/content_41874417.htm"
    private val detailUrl = "http://news.163.com/17/0607/21/CMC14QCD000189FH.html"

    private lateinit var pageResourceReference: PageResourceReference

    @Before
    fun setUp() {
        pageResourceReference = PageResourceReference("127.0.0.1", DEFAULT_PULSAR_MASTER_PORT)
        pageResourceReference.fetch(seedUrl)
        pageResourceReference.fetch(detailUrl)
    }

    @Test
    fun testLoadOutPages() {
        val url = "http://news.cjn.cn/gnxw/"
        val args = "-ps,-rpl,-prst,--expires=10m,-amin=8,-amax=1150,-umin=30,-umax=200"
        val args2 = "-ps,-rpl,-prst,--expires=1m"
        val start = 1
        val limit = 20
        val logLevel = 1

        var result: MutableMap<String, Any> = pageResourceReference.loadOutPages(url, args, args2, start, limit, logLevel)
        val totalCount = (result["totalCount"] as Double).toInt()
        val docs = result["docs"] as ArrayList<*>
        val debug = result["debug"] as Map<*, *>

        log.debug(debug.toString())

        result.remove("docs")
        result.remove("debug")
        assertTrue(!result.isEmpty())
        assertTrue(totalCount > 0)

        log.debug(result.toString())

        log.debug("The main page: ")
        result = pageResourceReference.load(url)
        log.debug(result.toString())
    }
}
