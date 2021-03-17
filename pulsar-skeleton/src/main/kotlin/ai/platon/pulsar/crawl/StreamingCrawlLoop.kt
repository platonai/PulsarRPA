package ai.platon.pulsar.crawl

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.crawl.common.GlobalCache
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

open class StreamingCrawlLoop(
    /**
     * The pulsar session
     * */
    val session: PulsarSession,
    /**
     * The global cache
     * */
    globalCache: GlobalCache,
    /**
     * Default LoadOptions for all load tasks within this crawl loop
     * */
    options: LoadOptions = session.options()
) : AbstractCrawlLoop(globalCache, options) {
    private val log = LoggerFactory.getLogger(StreamingCrawlLoop::class.java)

    private val isStarted = AtomicBoolean()
    private val isStopped = AtomicBoolean()
    private var crawlJob: Job? = null

    override val crawler by lazy {
        StreamingCrawler(fetchIterable.asSequence(), options, session, globalCache)
    }

    override fun start() {
        if (isStarted.get()) {
            log.warn("Crawl loop is already started")
            return
        }
        isStarted.set(true)

        if (collectors.isEmpty()) {
            fetchIterable.addDefaultCollectors()
        }

        val report = collectors.sortedBy { it.priority }.joinToString("\n") {
            "${it.priority} | <$it>"
        }
        log.info("Registered hyperlink collectors: \n$report")

        GlobalScope.launch {
            crawlJob = launch { crawler.run() }
        }
    }

    override fun stop() {
        if (!isStarted.get() || isStopped.get()) {
            return
        }
        isStopped.set(true)

        crawler.quit()
        runBlocking {
            crawlJob?.cancelAndJoin()
            crawlJob = null
        }
    }
}
