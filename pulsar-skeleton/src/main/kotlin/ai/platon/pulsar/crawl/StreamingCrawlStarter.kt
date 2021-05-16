package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.collect.FetchCache
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.GlobalCache
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

    @Volatile
    private var running = false
    private var crawlJob: Job? = null
    val isRunning get() = running

    var crawlEventHandler = DefaultCrawlEventHandler()

    override lateinit var crawler: StreamingCrawler<Hyperlink>

    val cacheManager get() = globalCache.fetchCacheManager
    val realTimeCache get() = cacheManager.realTimeCache
    val delayCache get() = cacheManager.delayCache
    val normalCacheSize get() = cacheManager.caches.entries.sumBy { it.value.size }
    val normalCacheEstimatedSize get() = cacheManager.caches.entries.sumBy { it.value.estimatedSize }
    val totalCacheSize get() = fetchIterable.cacheSize + realTimeCache.size + normalCacheSize

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

        if (collectors.isEmpty()) {
            fetchIterable.addDefaultCollectors()
        }

        val report = collectors.sortedBy { it.priority }.joinToString("\n") { c ->
            val priorityName = Priority13.values().firstOrNull { it.value == c.priority }?.name ?: ""
            String.format("%10d %s %s", c.priority, c.name, priorityName)
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
