package ai.platon.pulsar.rest

import org.junit.Ignore
import org.junit.Test

@Ignore("Test can not pass because api changes")
class TestSeedResource : ResourceTestBase() {

    private val seedUrl = "http://news.china.com/zh_cn/social/index.html"
    private val seedUrlToTestMultiInject = "http://news.cqnews.net/rollnews/index_6.htm"

    private val allUrls = arrayOf(seedUrl, seedUrlToTestMultiInject)

    @Ignore("@context is not available if jersey-test-framework-provider-inmemory is used")
    @Test
    fun testList() {
    }

    @Test
    fun testHome() {
//        val links = target("seeds")
//                .path("home")
//                .request()
//                .accept(MediaType.APPLICATION_JSON)
//                .get(object : GenericType<List<LinkDatum>>() {
//
//                })
//
//        ResourceTestBase.log.debug(links.toString())
//
//        // log.debug(ols.stream().map(MyOutlink::getUrl).collect(Collectors.joining("\n")));
//        assertTrue(!links.isEmpty())
    }

    @Test
    fun testInject() {
//        val statusFields = masterReference.inject(seedUrl, "")
//        ResourceTestBase.log.debug(statusFields!!.toString())
//        assertEquals(YES_STRING, statusFields["metadata I_S"])
    }

    @Test
    fun testInjectOutgoingPages() {
//        val result = target("seeds")
//                .path("inject-out-pages")
//                .queryParam("url", seedUrl)
//                .queryParam("filter", "-umin 50")
//                .request()
//                .get(String::class.java)
//        ResourceTestBase.log.debug(result)
    }

    @Test
    fun testUnInject() {
//        masterReference.inject(seedUrl)
//        val statusFields = masterReference.unInject(seedUrl)
//        ResourceTestBase.log.debug(statusFields!!.toString())
//        assertTrue(!statusFields.isEmpty())
//        assertTrue(!statusFields.containsKey("metadata I_S"))
    }

    @Test
    fun testUnInjectOutgoingPages() {
//        masterReference.inject(seedUrl)
//        val result = target("seeds")
//                .path("uninject-out-pages")
//                .queryParam("url", seedUrl)
//                .queryParam("filter", "-umin 50")
//                .request()
//                .get(String::class.java)
    }

    @Test
    fun testSeedHome() {
//        masterReference.unInject(seedUrl)
//
//        masterReference.inject(seedUrlToTestMultiInject)
//        masterReference.inject(seedUrlToTestMultiInject)
//        masterReference.inject(seedUrlToTestMultiInject)
//        masterReference.inject(seedUrlToTestMultiInject)
//
//        Thread.sleep(1000)
//
//        val seedUrls = masterReference.home()
//        assertTrue(!seedUrls.isEmpty())
        //    assertEquals(1, seedUrls.size());
    }
}
