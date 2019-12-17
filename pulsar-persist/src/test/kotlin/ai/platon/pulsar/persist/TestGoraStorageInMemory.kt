/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.platon.pulsar.persist

import ai.platon.pulsar.common.AppPaths
import ai.platon.pulsar.common.DateTimeUtil
import ai.platon.pulsar.common.Urls
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.persist.gora.GoraStorage
import ai.platon.pulsar.persist.gora.generated.GWebPage
import ai.platon.pulsar.persist.metadata.Mark
import ai.platon.pulsar.persist.metadata.Name
import org.apache.avro.util.Utf8
import org.apache.gora.store.DataStore
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path
import org.junit.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests basic Gora functionality by writing and reading webpages.
 */
/**
 * TODO: Test failed
 */
class TestGoraStorageInMemory {

    /**
     * Sequentially read and write pages to a store.
     *
     * @throws Exception
     */
    @Test // @Ignore("GORA-326 Removal of _g_dirty field from _ALL_FIELDS array and Field Enum in Persistent classes")
    @Throws(Exception::class)
    fun testSinglethreaded() {
        val id = "singlethread"
        readWriteGoraWebPage(id, store)
        readWriteWebPage(id, store)
    }

    /**
     * Tests multiple thread reading and writing to the same store, this should be
     * no problem because [DataStore] implementations claim to be thread
     * safe.
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    fun testMultithreaded() { // create a fixed thread pool
        val numThreads = 8
        val pool = Executors.newFixedThreadPool(numThreads)
        // define a list of tasks
        val tasks: MutableCollection<Callable<Int>> = ArrayList()
        for (i in 0 until numThreads) {
            tasks.add(Callable task@{
                try { // run a sequence
                    readWriteGoraWebPage(Thread.currentThread().name, store)
                    return@task 0
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@task 1
                }
            })
        }
        // submit them at once
        val results = pool.invokeAll(tasks)
        // check results
        for (result in results) {
            assertEquals(0, result.get() as Int)
        }
    }

    companion object {

        val LOG = LoggerFactory.getLogger(TestGoraStorage::class.java)

        private val conf = MutableConfig().also { it[CapabilityTypes.STORAGE_CRAWL_ID] = "test" }
        protected var testdir = Path(AppPaths.TEST_DIR.toString() + "/working")
        protected var fs: FileSystem = FileSystem.get(conf.unbox())
        protected var persistentDataStore = false

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
            LOG.debug("In shell: \nget '{}', '{}'", store.schemaName, Urls.reverseUrlOrEmpty(exampleUrl))
        }

        @Throws(Exception::class)
        private fun readWriteGoraWebPage(id: String, store: DataStore<String, GWebPage>?) {
            var page = GWebPage.newBuilder().build()
            val max = 500
            for (i in 0 until max) { // store a page with title
                val key = "key-$id-$i"
                val title = "title$i"
                page.pageTitle = Utf8(title)
                store!!.put(key, page)
                store.flush()
                // retrieve page and check title
                page = store[key]
                assertNotNull(page)
                assertEquals(title, page.pageTitle.toString())
            }
            // scan over the rows
            val result = store!!.execute(store.newQuery())
            var count = 0
            while (result.next()) {
                try { // only count keys in the store for the current id
                    if (result.key.contains(id)) ++count
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // check amount
            assertEquals(max.toLong(), count.toLong())
        }

        @Throws(Exception::class)
        private fun readWriteWebPage(id: String, store: DataStore<String, GWebPage>?) {
            val max = 1000
            for (i in 0 until max) {
                val url = AppConstants.SHORTEST_VALID_URL + "/" + id + "/" + i
                var page = WebPage.newWebPage(url)
                page.location = url
                page.pageText = "text"
                page.distance = 0
                page.headers.put("header1", "header1")
                page.marks.put(Mark.FETCH, "mark1")
                page.metadata[Name.CASH_KEY] = "metadata1"
                page.inlinks["http://www.a.com/1"] = ""
                page.inlinks["http://www.a.com/2"] = ""
                store!!.put(url, page.unbox())
                store.flush()
                // retrieve page and check title
                val goraPage = store[url]
                assertNotNull(goraPage)
                page = WebPage.box(url, goraPage)
                assertEquals("text", page.pageText)
                assertEquals(0, page.distance.toLong())
                assertEquals("header1", page.headers["header1"])
                // assertNotEquals("mark1", page.getMark(Mark.FETCH));
                assertEquals(Utf8("mark1"), page.marks[Mark.FETCH])
                assertEquals("metadata1", page.metadata.getOrDefault(Name.CASH_KEY, ""))
                assertEquals(2, page.inlinks.size.toLong())
            }
            // scan over the rows
            val result = store!!.execute(store.newQuery())
            var count = 0
            while (result.next()) {
                try { // only count keys in the store for the current id
                    if (result.key.contains(id)) {
                        ++count
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // check amount
            assertEquals(max.toLong(), count.toLong())
        }
    }
}