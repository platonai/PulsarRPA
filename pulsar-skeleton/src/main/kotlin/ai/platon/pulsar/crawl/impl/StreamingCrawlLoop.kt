package ai.platon.pulsar.crawl.impl

import ai.platon.pulsar.common.collect.UrlFeeder
import ai.platon.pulsar.common.config.CapabilityTypes.CRAWL_ENABLE_DEFAULT_DATA_COLLECTORS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.Crawler
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch
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

    private val running = AtomicBoolean()
    private val scope = CoroutineScope(Dispatchers.Default) + CoroutineName("sc")
    private var crawlJob: Job? = null
    private val started = CountDownLatch(1)

    val globalCache get() = globalCacheFactory.globalCache

    val isRunning get() = running.get()

    override val urlFeeder by lazy { createUrlFeeder() }

    private lateinit var actualCrawler: StreamingCrawler<UrlAware>

    override val crawler: Crawler get() = actualCrawler

    init {
        logger.info("Crawl loop is created | @{}", hashCode())
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
            actualCrawler.quit()
            runBlocking {
                crawlJob?.cancelAndJoin()
                crawlJob = null

                logger.info("Crawl loop #{} is stopped", id)
            }
        }
    }

    override fun await() {
        started.await()
        crawler.await()
    }

    private fun start0() {
        logger.info("Registered {} link collectors | loop#{} @{}", urlFeeder.collectors.size, id, hashCode())

        /**
         * The pulsar session
         * */
        val session = PulsarContexts.createSession()

        val urls = urlFeeder.asSequence()
        actualCrawler = StreamingCrawler(urls, defaultOptions, session, globalCacheFactory, noProxy = false)

        crawlJob = scope.launch {
            supervisorScope {
                started.countDown()
                actualCrawler.run(this)
            }
        }
    }

    private fun createUrlFeeder(): UrlFeeder {
        val enableDefaults = config.getBoolean(CRAWL_ENABLE_DEFAULT_DATA_COLLECTORS, true)
        return UrlFeeder(globalCache.urlPool, enableDefaults = enableDefaults)
    }
}
