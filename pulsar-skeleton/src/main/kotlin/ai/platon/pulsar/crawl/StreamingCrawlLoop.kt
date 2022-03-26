package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.collect.MultiSourceHyperlinkIterable
import ai.platon.pulsar.common.config.CapabilityTypes.ENABLE_DEFAULT_DATA_COLLECTORS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

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

    private var running = AtomicBoolean()
    private var crawlJob: Job? = null

    val globalCache get() = globalCacheFactory.globalCache

    val isRunning get() = running.get()

    var crawlEventHandler = DefaultCrawlEventHandler()

    override val fetchTaskIterable by lazy { createFetchTasks() }

    override lateinit var crawler: StreamingCrawler<UrlAware>
        protected set

    init {
        logger.info("Crawl loop is created | {}", this.javaClass.name + "@" + hashCode())
    }

    @Synchronized
    override fun start() {
        if (isRunning) {
            // issue a warning for debug
            logger.warn("Crawl loop #{} is already running", id)
        }

        if (running.compareAndSet(false, true)) {
            start0()
        }
    }

    @Synchronized
    override fun stop() {
        if (running.compareAndSet(true, false)) {
            // fetchIterable.clear()
            crawler.quit()
            runBlocking {
                crawlJob?.cancelAndJoin()
                crawlJob = null

                logger.info("Crawl loop #{} is stopped", id)
            }
        }
    }

    private fun start0() {
        logger.info("Registered {} hyperlink collectors | #{} @{}", fetchTaskIterable.collectors.size, id, hashCode())

        /**
         * The pulsar session
         * */
        val session = PulsarContexts.activate().createSession()

        val urls = fetchTaskIterable.asSequence()
        crawler = StreamingCrawler(urls,
            defaultOptions, session, globalCacheFactory, crawlEventHandler,
            noProxy = false
        )

        crawlJob = GlobalScope.launch {
            supervisorScope {
                crawler.run(this)
            }
        }
    }

    private fun createFetchTasks(): MultiSourceHyperlinkIterable {
        val enableDefaults = config.getBoolean(ENABLE_DEFAULT_DATA_COLLECTORS, true)
        return MultiSourceHyperlinkIterable(globalCache.fetchCaches, enableDefaults = enableDefaults)
    }
}
