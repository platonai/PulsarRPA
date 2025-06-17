package ai.platon.pulsar.persist

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.VolatileConfig
import ai.platon.pulsar.common.urls.URLUtils.reverseUrlOrEmpty
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.model.GoraWebPage
import com.google.common.collect.Lists
import org.apache.avro.util.Utf8
import org.apache.gora.memory.store.MemStore
import org.apache.gora.persistency.impl.DirtyCollectionWrapper
import org.apache.gora.persistency.impl.DirtyListWrapper
import org.apache.gora.store.DataStore
import org.junit.jupiter.api.AfterAll
import org.slf4j.LoggerFactory
import java.time.Instant
import kotlin.test.*

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 */
class TestGoraStorage {
    
    companion object {
        private val LOG = LoggerFactory.getLogger(TestGoraStorage::class.java)
        private val conf = VolatileConfig().also { it[CapabilityTypes.STORAGE_CRAWL_ID] = "test" }
        private val webDb = WebDb(conf)
        private var store: DataStore<String, GWebPage> = webDb.dataStorageFactory.getOrCreatePageStore()
        private var exampleUrl = AppConstants.EXAMPLE_URL + "/" + DateTimes.format(Instant.now(), "MMdd")
        
        @AfterAll
        @JvmStatic
        fun teardownClass() {
            webDb.delete(exampleUrl)
            webDb.flush()
            webDb.close()
            LOG.debug("In shell: \nget '{}', '{}'", store.schemaName, reverseUrlOrEmpty(exampleUrl))
        }
    }
    
    private val exampleUrls = IntRange(10000, 10050).map { AppConstants.EXAMPLE_URL + "/$it" }
    
    @BeforeTest
    fun setup() {
    }
    
    @AfterTest
    fun tearDown() {
    }
    
    @Test
    fun testWebDb() {
        if (store is MemStore) {
            return
        }
        
        println("Test with store " + store::class)
        
        val url = AppConstants.EXAMPLE_URL + "/" + Instant.now().toEpochMilli()
        var page = WebPageExt.newTestWebPage(url)
        assertEquals(url, page.url)
        // webDb.put(page.getUrl(), page, true);
        webDb.put(page)
        webDb.flush()
        
        page = webDb.get(url)
        val pageExt = WebPageExt(page)
        val page2 = webDb.get(url)
        assertEquals(page.url, page2.url)
        assertEquals(page.contentAsString, page2.contentAsString)
        assertTrue(page.isNotNil)
        pageExt.addLinks(exampleUrls)
        webDb.put(page)
        webDb.flush()
        
        val page3 = webDb.get(url)
        assertEquals(exampleUrls.size.toLong(), page3.links.size.toLong())
        pageExt.addLinks(exampleUrls)
        webDb.put(page)
        webDb.flush()
        
        val page4 = webDb.get(url)
        assertEquals(exampleUrls.size.toLong(), page4.links.size.toLong())
        webDb.delete(url)
        webDb.flush()
        
        page = webDb.get(url)
        assertTrue(page.isNil)
        webDb.delete(url)
    }
    
    @Test
    fun testModifyNestedSimpleArray() {
        if (store is MemStore) {
            return
        }
        
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
        page.links[i] = org.apache.avro.util.Utf8("")
        store.put(key, page)
        store.flush()
        
        page = store[key]
        assertNotNull(page)
        assertEquals("", page.links[i].toString())
    }

    @Test
    fun testClearNestedSimpleArray() {
        if (store is MemStore) {
            return
        }
        
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
        val links = DirtyListWrapper(
            Lists.newArrayList<CharSequence>(
                AppConstants.EXAMPLE_URL + "/-1",
                AppConstants.EXAMPLE_URL + "/-2",
                AppConstants.EXAMPLE_URL + "/1000000"
            )
        )
        page.links = links
        store.put(key, page)
        store.flush()
        page = store[key]
        assertNotNull(page)
        assertEquals(3, page.links.size.toLong())
    }

    @Test
    fun testUpdateNestedComplexArray() {
        if (store is MemStore) {
            return
        }
        
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
        if (store is MemStore) {
            return
        }
        
        createExamplePage()
        var page = webDb.get(exampleUrl)
        require(page is GoraWebPage)

        page.links = ArrayList()
        // page.getLinks().clear();
        assertTrue(page.links.isEmpty())
        assertTrue(page.unbox().isDirty)
        page.links.add(AppConstants.EXAMPLE_URL)
        page.links.add(AppConstants.EXAMPLE_URL + "/1")
        webDb.put(page, true)
        webDb.flush()
        page = webDb.get(exampleUrl)
        assertTrue(page.isNotNil)
        assertEquals(2, page.links.size.toLong())
    }
    
    @Test
    fun testUpdateNestedMap() {
        if (store is MemStore) {
            return
        }
        
        createExamplePage()
        var page = webDb.get(exampleUrl)
        page.inlinks.clear()
        assertTrue(page.inlinks.isEmpty())
        webDb.put(page)
        webDb.flush()
        page = webDb.get(exampleUrl)
        assertTrue(page.isNotNil)
        assertTrue(page.inlinks.isEmpty())
    }
    
    fun createExamplePage() {
        webDb.delete(exampleUrl)
        webDb.flush()
        
        LOG.debug("Random url: $exampleUrl")
        val page = GoraWebPage.newWebPage(exampleUrl, conf)
        
        for (i in 1..19) {
            val url = AppConstants.EXAMPLE_URL + "/" + i
            val url2 = AppConstants.EXAMPLE_URL + "/" + (i - 1)
            val link = HyperlinkPersistable.parse(url2).unbox()
            link.anchor = "test anchor ord:1"

            page.links.add(url2)
            page.inlinks[url] = url2
        }
        
        webDb.put(page)
        webDb.flush()
    }
}
