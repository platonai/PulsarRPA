package ai.platon.pulsar.crawl.common

import ai.platon.pulsar.common.DateTimes
import ai.platon.pulsar.common.MetricsSystem
import ai.platon.pulsar.common.Urls.reverseUrlOrEmpty
import ai.platon.pulsar.common.config.AppConstants
import ai.platon.pulsar.common.config.CapabilityTypes
import ai.platon.pulsar.common.config.MutableConfig
import ai.platon.pulsar.crawl.fetch.FetchTaskTracker
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.gora.generated.GWebPage
import org.apache.gora.store.DataStore
import org.junit.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.stream.Collectors
import java.util.stream.IntStream

/**
 * Created by vincent on 16-7-20.
 * Copyright @ 2013-2016 Platon AI. All rights reserved
 *
 * TODO: Test failed
 */
@Ignore("TODO: Test failed")
class TestWeakIndexer {
    private val conf: MutableConfig
    private val webDb: WebDb
    private val metricsSystem: MetricsSystem
    private val urlTrackerIndexer: WeakPageIndexer
    private val fetchTaskTracker: FetchTaskTracker
    private val store: DataStore<String, GWebPage>
    private val exampleUrls = IntStream.range(10000, 10050)
            .mapToObj { i: Int -> AppConstants.EXAMPLE_URL + "/" + i }
            .collect(Collectors.toList())
    private var exampleUrl: String? = null
    @Before
    fun setup() {
        conf["storage.data.store.class"] = AppConstants.TOY_STORE_CLASS
        exampleUrl = AppConstants.EXAMPLE_URL + "/" + DateTimes.format(Instant.now(), "MMdd")
    }

    @After
    fun teardown() { // webDb.delete(exampleUrl);
        webDb.flush()
        webDb.close()
        LOG.debug("In shell: \nget '{}', '{}'", store.schemaName, reverseUrlOrEmpty(exampleUrl!!))
    }

    @Ignore("TODO: Test failed")
    @Test
    fun testWeakPageIndexer() {
        val pageNo = 1
        val indexPageUrl = AppConstants.URL_TRACKER_HOME_URL + "/" + pageNo
        webDb.delete(indexPageUrl)
        webDb.flush()
        urlTrackerIndexer.indexAll(exampleUrls)
        urlTrackerIndexer.commit()
        var page = webDb.getOrNil(indexPageUrl)
        Assert.assertTrue(page.isNotNil)
        Assert.assertTrue(page.isInternal)
        Assert.assertEquals(exampleUrls.size.toLong(), page.liveLinks.size.toLong())
        urlTrackerIndexer.takeAll(pageNo)
        page = webDb.getOrNil(indexPageUrl)
        Assert.assertTrue(page.isNotNil)
        Assert.assertTrue(page.liveLinks.isEmpty())
    }

    companion object {
        val LOG = LoggerFactory.getLogger(TestWeakIndexer::class.java)
    }

    init {
        conf = MutableConfig()
        conf[CapabilityTypes.STORAGE_CRAWL_ID] = "test"
        webDb = WebDb(conf)
        metricsSystem = MetricsSystem(webDb, conf)
        fetchTaskTracker = FetchTaskTracker(webDb, metricsSystem, conf)
        urlTrackerIndexer = WeakPageIndexer(AppConstants.URL_TRACKER_HOME_URL, webDb)
        store = webDb.store
    }
}
