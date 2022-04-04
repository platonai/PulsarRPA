package ai.platon.pulsar.crawl

import ai.platon.pulsar.common.collect.UrlFeeder
import ai.platon.pulsar.common.config.CapabilityTypes.CRAWL_ENABLE_DEFAULT_DATA_COLLECTORS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.sleepSeconds
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.common.GlobalCacheFactory
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
    private val scope = CoroutineScope(Dispatchers.Default)
    private var crawlJob: Job? = null
    private val started = CountDownLatch(1)

    val globalCache get() = globalCacheFactory.globalCache

    val isRunning get() = running.get()

    var crawlEventHandler = DefaultCrawlEventHandler()

    override val urlFeeder by lazy { createHyperlinkFeeder() }

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
            crawler.quit()
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
        logger.info("Registered {} hyperlink collectors | #{} @{}", urlFeeder.collectors.size, id, hashCode())

        /**
         * The pulsar session
         * */
        val session = PulsarContexts.createSession()

        val urls = urlFeeder.asSequence()
        crawler = StreamingCrawler(urls,
            defaultOptions, session, globalCacheFactory, crawlEventHandler,
            noProxy = false
        )

        crawlJob = scope.launch {
            supervisorScope {
                started.countDown()
                crawler.run(this)
            }
        }
    }

    private fun createHyperlinkFeeder(): UrlFeeder {
        val enableDefaults = config.getBoolean(CRAWL_ENABLE_DEFAULT_DATA_COLLECTORS, true)
        return UrlFeeder(globalCache.urlPool, enableDefaults = enableDefaults)
    }
}
