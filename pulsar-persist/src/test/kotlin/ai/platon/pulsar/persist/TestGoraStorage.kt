package ai.platon.pulsar.persist

import ai.platon.pulsar.common.DateTimeUtil
import ai.platon.pulsar.common.Urls.reverseUrlOrEmpty
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.persist.HypeLink
import ai.platon.pulsar.persist.gora.generated.GHypeLink
import ai.platon.pulsar.persist.gora.generated.GWebPage
import com.google.common.collect.Lists
import org.apache.avro.util.Utf8
import org.apache.gora.persistency.impl.DirtyCollectionWrapper
import org.apache.gora.persistency.impl.DirtyListWrapper
import org.apache.gora.store.DataStore
import org.junit.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import java.util.stream.Collectors
import java.util.stream.IntStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 *
 * TODO: Test failed
 */
class TestGoraStorage {

    companion object {
        val LOG = LoggerFactory.getLogger(TestGoraStorage::class.java)
        private val conf = MutableConfig().also { it[CapabilityTypes.STORAGE_CRAWL_ID] = "test" }
        private val webDb = WebDb(conf)
        private var store: DataStore<String, GWebPage> = webDb.store
        private var exampleUrl = AppConstants.EXAMPLE_URL + "/" + DateTimeUtil.format(Instant.now(), "MMdd")

        @BeforeClass
        fun setupClass() {
        }

        @AfterClass
        fun teardownClass() {
            webDb.delete(exampleUrl)
            webDb.flush()
            webDb.close()
            LOG.debug("In shell: \nget '{}', '{}'", store.schemaName, reverseUrlOrEmpty(exampleUrl))
        }
    }

    private val exampleUrls = IntStream.range(10000, 10050)
            .mapToObj { i: Int -> AppConstants.EXAMPLE_URL + "/" + i }
            .collect(Collectors.toList())

    @Before
    fun setup() {
    }

    @After
    fun teardown() {
    }

    @Test
    fun testWebDb() {
        val url = AppConstants.EXAMPLE_URL + "/" + Instant.now().toEpochMilli()
        var page = WebPage.newInternalPage(url)
        assertEquals(url, page.url)
        // webDb.put(page.getUrl(), page, true);
        webDb.put(page)
        webDb.flush()
        page = webDb.getOrNil(url)
        val page2 = webDb.getOrNil(url)
        assertEquals(page.url, page2.url)
        assertEquals(page.contentAsString, page2.contentAsString)
        assertTrue(page.isNotNil)
        assertTrue(page.isInternal)
        page.addLinks(exampleUrls)
        webDb.put(page)
        webDb.flush()
        val page3 = webDb.getOrNil(url)
        assertEquals(exampleUrls.size.toLong(), page3.links.size.toLong())
        page.addLinks(exampleUrls)
        webDb.put(page)
        webDb.flush()
        val page4 = webDb.getOrNil(url)
        assertEquals(exampleUrls.size.toLong(), page4.links.size.toLong())
        webDb.delete(url)
        webDb.flush()
        page = webDb.getOrNil(url)
        assertTrue(page.isNil)
        webDb.delete(url)
    }

    @Test
    fun testModifyNestedSimpleArray() {
        createExamplePage()
        val key = reverseUrlOrEmpty(exampleUrl)
        var page = store[key]
        assertNotNull(page)
        var i = 0
        assertTrue(page.links[i] is Utf8)
        val modifiedLink = AppConstants.EXAMPLE_URL + "/" + "0-modified"
        page.links[i] = modifiedLink
        store.put(key, page)
        store.flush()
        page = store[key]
        assertNotNull(page)
        assertEquals(modifiedLink, page.links[i].toString())
        i = 1
        page.links[i] = Utf8()
        store.put(key, page)
        store.flush()
        page = store[key]
        assertNotNull(page)
        assertEquals("", page.links[i].toString())
        i = 2
        page.links[i] = ""
        store.put(key, page)
        store.flush()
        page = store[key]
        assertNotNull(page)
        assertEquals("", page.links[i].toString())
        i = 3
        page.links[i] = Utf8("")
        store.put(key, page)
        store.flush()
        page = store[key]
        assertNotNull(page)
        assertEquals("", page.links[i].toString())
    }

    /**
     * TODO: We can not clear an array, HBase keeps unchanged
     */
    @Test
    fun testClearNestedSimpleArray() {
        createExamplePage()
        val key = reverseUrlOrEmpty(exampleUrl)
        var page = store[key]
        assertNotNull(page)
        assertTrue(page.links[0] is Utf8)
        page.links.clear()
        assertTrue(page.links.isEmpty())
        assertTrue(page.isLinksDirty)
        assertTrue(page.links is DirtyCollectionWrapper<*>)
        val wrapper = page.links as DirtyCollectionWrapper<*>
        assertTrue(wrapper.isDirty)
        assertTrue(wrapper.isEmpty())
        val links = DirtyListWrapper(Lists.newArrayList<CharSequence>(AppConstants.EXAMPLE_URL + "/-1", AppConstants.EXAMPLE_URL + "/-2", AppConstants.EXAMPLE_URL + "/1000000"))
        page.links = links
        store.put(key, page)
        store.flush()
        page = store[key]
        assertNotNull(page)
        assertEquals(3, page.links.size.toLong())
    }

    /**
     * TODO: We can not clear an array, HBase keeps unchanged
     */
    @Test
    fun testUpdateNestedComplexArray() {
        createExamplePage()

        val key = reverseUrlOrEmpty(exampleUrl)
        var page = store[key]
        assertNotNull(page)

        assertTrue(page.liveLinks.isNotEmpty())
        println(page.liveLinks.values.first().anchor.javaClass)
        assertTrue(page.liveLinks.values.first().anchor is Utf8)

        page.liveLinks.clear()
        assertTrue(page.liveLinks.isEmpty())

        store.put(key, page)
        store.flush()

        page = store[key]
        assertNotNull(page)
        assertTrue(page.liveLinks.isEmpty())
    }

    @Test
    fun testUpdateNestedArray2() {
        createExamplePage()
        var page = webDb.getOrNil(exampleUrl)
        page.links = ArrayList()
        // page.getLinks().clear();
        assertTrue(page.links.isEmpty())
        assertTrue(page.unbox().isDirty)
        page.links.add(AppConstants.EXAMPLE_URL)
        page.links.add(AppConstants.EXAMPLE_URL + "/1")
        webDb.put(page, true)
        webDb.flush()
        page = webDb.getOrNil(exampleUrl)
        assertTrue(page.isNotNil)
        assertEquals(2, page.links.size.toLong())
    }

    @Test
    fun testUpdateNestedMap() {
        createExamplePage()
        var page = webDb.getOrNil(exampleUrl)
        page.inlinks.clear()
        assertTrue(page.inlinks.isEmpty())
        webDb.put(page)
        webDb.flush()
        page = webDb.getOrNil(exampleUrl)
        assertTrue(page.isNotNil)
        assertTrue(page.inlinks.isEmpty())
    }

    fun createExamplePage() {
        webDb.delete(exampleUrl)
        webDb.flush()

        LOG.debug("Random url: $exampleUrl")
        val page = WebPage.newWebPage(exampleUrl)

        for (i in 1..19) {
            val url = AppConstants.EXAMPLE_URL + "/" + i
            val url2 = AppConstants.EXAMPLE_URL + "/" + (i - 1)
            val link = HypeLink.parse(url2).unbox()
            link.anchor = "test anchor ord:1"

            page.liveLinks[link.url] = link
            page.liveLinks = page.liveLinks
            page.links.add(url2)
            page.inlinks[url] = url2
        }

        webDb.put(page)
        webDb.flush()
    }
}