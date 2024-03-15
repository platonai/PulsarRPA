package ai.platon.pulsar.crawl.impl

import ai.platon.pulsar.common.collect.UrlFeeder
import ai.platon.pulsar.common.config.CapabilityTypes.CRAWL_ENABLE_DEFAULT_DATA_COLLECTORS
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.urls.UrlAware
import ai.platon.pulsar.context.PulsarContexts
import ai.platon.pulsar.crawl.Crawler
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

open class StreamingCrawlLoop(
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

    val isRunning get() = running.get()

    override val urlFeeder by lazy { createUrlFeeder() }

    private lateinit var _crawler: StreamingCrawler
    override val crawler: Crawler get() = _crawler

    // TODO: better initialization, may use spring bean
    private val context get() = PulsarContexts.create()

    init {
        logger.info("Crawl loop is created | {} | @{}", display, hashCode())
    }

    @Synchronized
    override fun start() {
        if (isRunning) {
            // issue a warning for debug
            logger.warn("Crawl loop {} is already running", display)
        }

        if (running.compareAndSet(false, true)) {
            start0()
        }
    }

    @Synchronized
    override fun stop() {
        if (running.compareAndSet(true, false)) {
            _crawler.close()
            runBlocking {
                crawlJob?.cancelAndJoin()
                crawlJob = null

                logger.info("Crawl loop {} is stopped", display)
            }
        }
    }

    /**
     * Wait until the loop is started and all tasks are done.
     * */
    @Throws(InterruptedException::class)
    override fun await() {
        started.await()
        crawler.await()
    }

    private fun start0() {
        logger.info("Registered {} link collectors | {} | @{}", urlFeeder.collectors.size, display, hashCode())

        val urls = urlFeeder.asSequence()
        _crawler = StreamingCrawler(urls, context.createSession())

        crawlJob = scope.launch {
            supervisorScope {
                started.countDown()
                _crawler.run(this)
            }
        }
    }

    private fun createUrlFeeder(): UrlFeeder {
        val enableDefaults = config.getBoolean(CRAWL_ENABLE_DEFAULT_DATA_COLLECTORS, true)
        return UrlFeeder(context.crawlPool, enableDefaults = enableDefaults)
    }
}
