package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.collect.PriorityDataCollector
import ai.platon.pulsar.common.config.CapabilityTypes.ENABLE_DEFAULT_DATA_COLLECTORS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.GlobalCache
import ai.platon.pulsar.crawl.common.collect.PriorityDataCollectorsFormatter
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

open class StreamingCrawlLoop(
    /**
     * The global cache
     * */
    globalCache: GlobalCache,
    unmodifiedConfig: ImmutableConfig
) : AbstractCrawlLoop(globalCache, unmodifiedConfig) {
    private val log = LoggerFactory.getLogger(StreamingCrawlLoop::class.java)

    private val enableDefaultCollectors
        get() = unmodifiedConfig.getBoolean(ENABLE_DEFAULT_DATA_COLLECTORS, true)
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

    @Synchronized
    override fun start() {
        if (running) {
            log.warn("Crawl loop is already running")
            return
        }
        running = true

        if (enableDefaultCollectors && collectors.isEmpty()) {
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
}
