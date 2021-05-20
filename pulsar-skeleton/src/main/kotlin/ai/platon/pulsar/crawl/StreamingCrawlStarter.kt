package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.collect.FetchCache
import ai.platon.pulsar.common.collect.PriorityDataCollector
import ai.platon.pulsar.common.config.CapabilityTypes.CREATE_DEFAULT_DATA_COLLECTORS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.crawl.common.collect.PriorityDataCollectorsFormatter
import com.codahale.metrics.Gauge
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

open class StreamingCrawlStarter(
    /**
     * The global cache
     * */
    globalCache: GlobalCache,
    unmodifiedConfig: ImmutableConfig
) : AbstractCrawlStarter(globalCache, unmodifiedConfig) {
    private val log = LoggerFactory.getLogger(StreamingCrawlStarter::class.java)

    private val createDefaultCollectors
        get() = unmodifiedConfig.getBoolean(CREATE_DEFAULT_DATA_COLLECTORS, true)
    @Volatile
    private var running = false
    private var crawlJob: Job? = null
    val isRunning get() = running

    var crawlEventHandler = DefaultCrawlEventHandler()

    override lateinit var crawler: StreamingCrawler<UrlAware>

    val cacheManager get() = globalCache.fetchCacheManager
    val realTimeCache get() = cacheManager.realTimeCache
    val delayCache get() = cacheManager.delayCache
    val normalCacheSize get() = cacheManager.caches.entries.sumBy { it.value.size }
    val normalCacheEstimatedSize get() = cacheManager.caches.entries.sumBy { it.value.estimatedSize }
    val totalCacheSize get() = fetchIterable.cacheSize + realTimeCache.size + normalCacheSize

    val allCollectors: List<PriorityDataCollector<UrlAware>> get() {
        val loadingIterable = fetchIterable.loadingIterable
        val allCollectors = listOfNotNull(loadingIterable.realTimeCollector, loadingIterable.delayCollector)
            .filterIsInstance<PriorityDataCollector<UrlAware>>()
            .toMutableList()
        this.collectors.toCollection(allCollectors)
        allCollectors.sortBy { it.priority }
        return allCollectors
    }

    override val abstract: String get() = PriorityDataCollectorsFormatter(allCollectors).abstract()

    override val report: String get() = PriorityDataCollectorsFormatter(allCollectors).toString()

    private val gauges = mapOf(
        "fetchIterableCacheSize" to Gauge { fetchIterable.cacheSize },
        "realTimeCacheSize" to Gauge { realTimeCache.size },
        "delayCacheSize" to Gauge { delayCache.size },
        "normalCacheSize" to Gauge { normalCacheSize },
        "normalCacheEstimatedSize" to Gauge { normalCacheEstimatedSize },
        "normalCacheReport" to Gauge { getNormalCacheReport() },
        "totalCacheSize" to Gauge { totalCacheSize },
        "numCollectors" to Gauge { collectors.size }
    )

    init {
        // There might be several StreamingCrawlerStarter instances
        // AppMetrics.reg.registerAll(this, gauges)
    }

    @Synchronized
    override fun start() {
        // TODO: check preconditions
        
        if (running) {
            log.warn("Crawl loop is already running")
            return
        }
        running = true

        if (createDefaultCollectors && collectors.isEmpty()) {
            fetchIterable.addDefaultCollectors()
        }

        log.debug("Registered hyperlink collectors: \n$report")

        /**
         * The pulsar session
         * */
        val session = PulsarContexts.activate().createSession()

        val urls = fetchIterable.asSequence()
        crawler = StreamingCrawler(urls, defaultOptions, session, globalCache, crawlEventHandler)
        crawler.proxyPool = session.context.getBean()

        GlobalScope.launch {
            crawlJob = launch { crawler.run() }
        }
    }

    @Synchronized
    override fun stop() {
        if (!running) {
            return
        }
        running = false

        collectors.clear()
        crawler.quit()
        runBlocking {
            crawlJob?.cancelAndJoin()
            crawlJob = null

            log.info("Streaming crawler is stopped")
        }
    }

    private fun getNormalCacheReport(): String {
        val sizeReport: (FetchCache) -> String = {
            it.queues.joinToString { it.size.toString() }
        }

        return cacheManager.caches.values
            .map { it.name to it }
            .filter { it.second.size != 0 }
            .joinToString(", ") { "${it.first}: " + sizeReport(it.second) }
    }
}
