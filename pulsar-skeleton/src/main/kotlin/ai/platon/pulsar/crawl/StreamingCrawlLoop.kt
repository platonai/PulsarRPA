package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.collect.MultiSourceHyperlinkIterable
import ai.platon.pulsar.common.config.CapabilityTypes.ENABLE_DEFAULT_DATA_COLLECTORS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

open class StreamingCrawlLoop(
    /**
     * The global cache
     * */
    val globalCacheFactory: GlobalCacheFactory,
    /**
     * The unmodified configuration load from file
     * */
    unmodifiedConfig: ImmutableConfig,
    /**
     * The loop name
     * */
    name: String = "StreamingCrawlLoop"
) : AbstractCrawlLoop(name, unmodifiedConfig) {
    private val logger = LoggerFactory.getLogger(StreamingCrawlLoop::class.java)

    val enableDefaultCollectors
        get() = unmodifiedConfig.getBoolean(ENABLE_DEFAULT_DATA_COLLECTORS, true)

    @Volatile
    private var running = false
    private var crawlJob: Job? = null

    val globalCache get() = globalCacheFactory.globalCache

    val isRunning get() = running

    var crawlEventHandler = DefaultCrawlEventHandler()

    override val fetchIterable by lazy {
        MultiSourceHyperlinkIterable(globalCache.fetchCaches, enableDefaults = enableDefaultCollectors)
    }

    override lateinit var crawler: StreamingCrawler<UrlAware>
        protected set

    init {
        logger.info("Streaming crawl loop is created | {}", this.javaClass.name + "@" + hashCode())
    }

    @Synchronized
    override fun start() {
        if (running) {
            logger.warn("Crawl loop #{} is already running", id)
            return
        }
        running = true

        logger.info("Registered {} hyperlink collectors | #{} @{}", fetchIterable.collectors.size, id, hashCode())

        /**
         * The pulsar session
         * */
        val session = PulsarContexts.activate().createSession()

        val urls = fetchIterable.asSequence()
        crawler = StreamingCrawler(urls, defaultOptions, session, globalCacheFactory, crawlEventHandler)
        crawler.proxyPool = session.context.getBeanOrNull()

        crawlJob = GlobalScope.launch { crawler.run() }
    }

    @Synchronized
    override fun stop() {
        if (!running) {
            return
        }
        running = false

        // fetchIterable.clear()
        crawler.quit()
        runBlocking {
            crawlJob?.cancelAndJoin()
            crawlJob = null

            logger.info("Streaming crawler #{} is stopped", id)
        }
    }
}
