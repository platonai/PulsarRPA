package ai.platon.pulsar.crawl

import ai.platon.pulsar.PulsarSession
import ai.platon.pulsar.common.Priority13
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.common.urls.Hyperlink
import ai.platon.pulsar.crawl.common.GlobalCache
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

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
    options: LoadOptions = session.options(),
) : AbstractCrawlLoop(globalCache, options) {
    private val log = LoggerFactory.getLogger(StreamingCrawlLoop::class.java)

    @Volatile
    private var running = false
    private var crawlJob: Job? = null
    val isRunning get() = running

    //    override val crawler by lazy {
//        StreamingCrawler(fetchIterable.asSequence(), options, session, globalCache)
//    }
    override lateinit var crawler: StreamingCrawler<Hyperlink>

    @Synchronized
    override fun start() {
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
        log.info("Registered hyperlink collectors: \n$report")

        val urls = fetchIterable.asSequence()
        crawler = StreamingCrawler(urls, crawlOptions, session, globalCache)

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
        }
    }

}
