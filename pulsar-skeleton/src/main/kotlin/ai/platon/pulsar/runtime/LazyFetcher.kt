package ai.platon.pulsar.runtime

import ai.platon.pulsar.common.concurrent.ScheduledMonitor
import ai.platon.pulsar.common.config.CapabilityTypes.FETCH_CONCURRENCY
import ai.platon.pulsar.common.options.LoadOptions
import ai.platon.pulsar.context.PulsarContext
import ai.platon.pulsar.context.support.AbstractPulsarContext
import ai.platon.pulsar.crawl.fetch.LazyFetchTaskManager
import ai.platon.pulsar.persist.metadata.FetchMode
import org.slf4j.LoggerFactory

class LazyFetcher(val pulsarContext: AbstractPulsarContext): ScheduledMonitor() {
    private val log = LoggerFactory.getLogger(LazyFetcher::class.java)

    private val backgroundSession = pulsarContext.createSession()
    private val lazyFetchTaskManager = pulsarContext.getBean<LazyFetchTaskManager>()
    private val config = backgroundSession.sessionConfig
    private val backgroundTaskBatchSize = config.getUint(FETCH_CONCURRENCY, 20)
    private var lazyTaskRound = 0

    init {
        backgroundSession.disableCache()
    }

    override fun watch() {
        loadLazyTasks()
        fetchSeeds()
    }

    /**
     * Get background tasks and run them
     * TODO: should handle lazy tasks in a better place
     */
    private fun loadLazyTasks() {
        for (mode in FetchMode.values()) {
            val urls = lazyFetchTaskManager.takeLazyTasks(mode, backgroundTaskBatchSize).map { it.toString() }
            if (urls.isNotEmpty()) {
                loadAll(urls, backgroundTaskBatchSize, mode)
            }
        }
    }

    /**
     * Get periodical tasks and run them
     */
    private fun fetchSeeds() {
        for (mode in FetchMode.values()) {
            val urls = lazyFetchTaskManager.getSeeds(mode, 1000)
            if (urls.isNotEmpty()) {
                loadAll(urls, backgroundTaskBatchSize, mode)
            }
        }
    }

    private fun loadAll(urls: Iterable<String>, batchSize: Int, mode: FetchMode) {
        if (!urls.iterator().hasNext() || batchSize <= 0) {
            return
        }

        val options = LoadOptions.create().apply {
            fetchMode = mode
            background = true
        }

        // TODO: lower priority
        urls.asSequence().chunked(batchSize).forEach { loadAll(it, options) }
    }

    private fun loadAll(urls: Collection<String>, loadOptions: LoadOptions) {
        ++lazyTaskRound
        log.debug("Running {}th round for lazy tasks", lazyTaskRound)
        backgroundSession.parallelLoadAll(urls, loadOptions)
    }
}
