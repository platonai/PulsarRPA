package org.warps.pulsar.persist

import org.apache.avro.util.Utf8
import org.apache.gora.store.DataStore
import org.apache.gora.util.GoraException
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.warps.pulsar.common.PulsarConstants
import org.warps.pulsar.common.PulsarConstants.*
import org.warps.pulsar.common.config.ImmutableConfig
import org.warps.pulsar.persist.gora.GoraStorage
import org.warps.pulsar.persist.gora.db.WebDb
import org.warps.pulsar.persist.gora.generated.GWebPage
import java.nio.file.Paths
import kotlin.streams.toList

/**
 * Created by vincent on 17-7-26.
 * Copyright @ 2013-2017 Warpspeed Information. All rights reserved
 */
class TestWebPage {
    protected var testdir = Path("/tmp/pulsar-" + System.getenv("USER") + "/test")
    protected var persistentDataStore = false

    protected lateinit var conf: ImmutableConfig
    protected lateinit var fs: FileSystem
    protected lateinit var store: DataStore<String, GWebPage>
    protected lateinit var webDb: WebDb

    @Before
    fun setUp() {
        conf = ImmutableConfig()
        // conf.set("storage.data.store.class", "org.apache.gora.memory.store.MemStore")
        fs = FileSystem.get(conf.unbox())
        webDb = WebDb(conf)
        store = webDb.store
    }

    @After
    fun tearDown() {
        // empty the database after test
        if (!persistentDataStore) {
            store.deleteByQuery(store.newQuery())
            store.flush()
            store.close()
        }
        fs.delete(testdir, true)
    }

    @Test
    @Throws(GoraException::class, ClassNotFoundException::class)
    fun testStringTypes() {
        val page = WebPage.newWebPage(EXAMPLE_URL)
        val key = page.key
        val url = page.url

        page.pageTitle = "Let it go"
        store.put(key, page.unbox())
        store.flush()
        store.close()

        store = GoraStorage.createDataStore(conf.unbox(), String::class.java, GWebPage::class.java)
        val goraWebPage = store.get(key)
        println(goraWebPage.pageTitle.javaClass)
        println(goraWebPage.pageTitle)
        // For HBase store:
        if (store is org.apache.gora.hbase.store.HBaseStore) {
            assertTrue(goraWebPage.pageTitle is Utf8)
            assertNotEquals(page.pageTitle, goraWebPage.pageTitle)
        } else if (store is org.apache.gora.memory.store.MemStore) {
            assertTrue(goraWebPage.pageTitle is String)
            assertEquals(page.pageTitle, goraWebPage.pageTitle)
        }
    }

    @Test
    fun testAddLinks() {
        val page = WebPage.newWebPage(EXAMPLE_URL)
        val testLinks = mutableListOf<String>()
        for (i in 0..19) {
            testLinks.add("$EXAMPLE_URL/$i")
        }
        page.addHyperLinks(testLinks.map { it -> HypeLink(it) })
        val size = page.links.size

        page.addHyperLinks(testLinks.map { it -> HypeLink(it) })
        val size2 = page.links.size
        assertEquals(size.toLong(), size2.toLong())

        // Every string load from backend storage is a Utf8
        val links = page.links.stream().map { l -> WebPage.u8(l.toString()) }.toList()
        page.links = links
        page.addHyperLinks(testLinks.map { it -> HypeLink(it) })
        val size3 = page.links.size
        assertEquals(size.toLong(), size3.toLong())
    }

    private fun prepareScanData() {
        val baseUrl = "$EXAMPLE_URL/test-scan"
        val testUrls = IntRange(1, 20).map { "$baseUrl/$it" }

        webDb.delete(baseUrl)
        testUrls.map { webDb.delete(it) }
        webDb.flush()

        webDb.put(baseUrl, WebPage.newInternalPage(baseUrl))
        testUrls.map { WebPage.newInternalPage(it) }.map { webDb.put(it.url, it) }
        webDb.flush()
    }

    @Test
    fun testScan() {
        prepareScanData()

        val baseUrl = "$EXAMPLE_URL/test-scan"
        val testUrls = IntRange(1, 20).map { "$baseUrl/$it" }
        val lastUrl = "$EXAMPLE_URL/$UNICODE_LAST_CODE_POINT"

        val pages = webDb.scan(baseUrl).iterator().asSequence().toList()
        val urls = pages.map { it.url }

        urls.forEach { println(it) }

        assertEquals(1 + testUrls.size, pages.size)

        assertTrue(urls.contains(testUrls.first()))
        assertTrue(urls.contains(testUrls.last()))
        assertFalse(urls.contains(lastUrl))
    }
}
