package ai.platon.pulsar.rest

import ai.platon.pulsar.common.config.PulsarConstants.YES_STRING
import ai.platon.pulsar.rest.model.response.LinkDatum
import ai.platon.pulsar.rest.rpc.MasterReference
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import java.util.ArrayList
import javax.ws.rs.core.GenericType
import javax.ws.rs.core.MediaType
import kotlin.test.assertEquals
import kotlin.test.assertTrue


@Ignore("Test can not pass because api changes")
class TestSeedResource : ResourceTestBase() {

    private val seedUrl = "http://news.china.com/zh_cn/social/index.html"
    private val seedUrlToTestMultiInject = "http://news.cqnews.net/rollnews/index_6.htm"

    private val allUrls = arrayOf(seedUrl, seedUrlToTestMultiInject)

    protected lateinit var masterReference: MasterReference

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        masterReference = MasterReference(baseUri, client())
        masterReference.inject(seedUrl)
    }

    @Ignore("@context is not available if jersey-test-framework-provider-inmemory is used")
    @Test
    fun testList() {
        val result = target("seeds")
                .request()
                .accept(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON)
                .get(String::class.java)

        val gson = GsonBuilder().create()
        val listType = object : TypeToken<ArrayList<LinkDatum>>() {

        }.type
        val ols = gson.fromJson<ArrayList<LinkDatum>>(result, listType)

        // log.debug(ols.stream().map(MyOutlink::getUrl).collect(Collectors.joining("\n")));
        assertTrue(!ols.isEmpty())
    }

    @Test
    fun testHome() {
        val links = target("seeds")
                .path("home")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get(object : GenericType<List<LinkDatum>>() {

                })

        ResourceTestBase.log.debug(links.toString())

        // log.debug(ols.stream().map(MyOutlink::getUrl).collect(Collectors.joining("\n")));
        assertTrue(!links.isEmpty())
    }

    @Test
    fun testInject() {
        val statusFields = masterReference.inject(seedUrl, "")
        ResourceTestBase.log.debug(statusFields!!.toString())
        assertEquals(YES_STRING, statusFields["metadata I_S"])
    }

    @Test
    fun testInjectOutgoingPages() {
        val result = target("seeds")
                .path("inject-out-pages")
                .queryParam("url", seedUrl)
                .queryParam("filter", "-umin 50")
                .request()
                .get(String::class.java)
        ResourceTestBase.log.debug(result)
    }

    @Test
    fun testUnInject() {
        masterReference.inject(seedUrl)
        val statusFields = masterReference.unInject(seedUrl)
        ResourceTestBase.log.debug(statusFields!!.toString())
        assertTrue(!statusFields.isEmpty())
        assertTrue(!statusFields.containsKey("metadata I_S"))
    }

    @Test
    fun testUnInjectOutgoingPages() {
        masterReference.inject(seedUrl)
        val result = target("seeds")
                .path("uninject-out-pages")
                .queryParam("url", seedUrl)
                .queryParam("filter", "-umin 50")
                .request()
                .get(String::class.java)
        ResourceTestBase.log.debug(result)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testSeedHome() {
        masterReference.unInject(seedUrl)

        masterReference.inject(seedUrlToTestMultiInject)
        masterReference.inject(seedUrlToTestMultiInject)
        masterReference.inject(seedUrlToTestMultiInject)
        masterReference.inject(seedUrlToTestMultiInject)

        Thread.sleep(1000)

        val seedUrls = masterReference.home()
        assertTrue(!seedUrls.isEmpty())
        //    assertEquals(1, seedUrls.size());
    }

    @After
    @Throws(Exception::class)
    override fun tearDown() {
        // Stream.of(allUrls).forEach(pageResourceReference::delete);
        super.tearDown()
    }
}
