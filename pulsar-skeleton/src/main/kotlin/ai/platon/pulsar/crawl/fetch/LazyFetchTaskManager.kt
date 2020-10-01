package ai.platon.pulsar.crawl.fetch

import ai.platon.pulsar.common.config.AppConstants.SEED_HOME_URL
import ai.platon.pulsar.common.config.AppConstants.URL_TRACKER_HOME_URL
import ai.platon.pulsar.common.config.ImmutableConfig
import ai.platon.pulsar.common.config.Parameterized
import ai.platon.pulsar.crawl.common.WeakPageIndexer
import ai.platon.pulsar.persist.WebDb
import ai.platon.pulsar.persist.WebPage
import ai.platon.pulsar.persist.metadata.FetchMode
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

class LazyFetchTaskManager(
        private val webDb: WebDb,
        private val fetchMetrics: FetchMetrics,
        conf: ImmutableConfig
): Parameterized, AutoCloseable {

    companion object {
        const val LAZY_FETCH_URLS_PAGE_BASE = 100
        const val TIMEOUT_URLS_PAGE = 1000
        const val FAILED_URLS_PAGE = 1001
        const val DEAD_URLS_PAGE = 1002
    }

    private val log = LoggerFactory.getLogger(LazyFetchTaskManager::class.java)!!
    private val seedIndexer = WeakPageIndexer(SEED_HOME_URL, webDb)
    private val urlTrackerIndexer = WeakPageIndexer(URL_TRACKER_HOME_URL, webDb)

    val startTime = Instant.now()
    val elapsedTime get() = Duration.between(startTime, Instant.now())

    private val timeoutUrls = fetchMetrics.timeoutUrls
    private val failedUrls = fetchMetrics.failedUrls
    private val deadUrls = fetchMetrics.deadUrls

    private val closed = AtomicBoolean()

    init {
        urlTrackerIndexer.takeAll(TIMEOUT_URLS_PAGE).mapTo(timeoutUrls) { it.toString() }
        urlTrackerIndexer.getAll(FAILED_URLS_PAGE).mapTo(failedUrls) { it.toString() }
        urlTrackerIndexer.getAll(DEAD_URLS_PAGE).mapTo(deadUrls) { it.toString() }

        timeoutUrls.clear()
        failedUrls.clear()
        deadUrls.clear()

        // params.withLogger(log).info(true)
    }

    fun getSeeds(mode: FetchMode, limit: Int): Set<String> {
        val pageNo = 1
        val now = Instant.now()
        return seedIndexer.getAll(pageNo)
                .asSequence()
                .mapNotNull { webDb.getOrNull(it.toString()) }
                .filter { it.fetchMode == mode && it.fetchTime.isBefore(now) }
                .take(limit).mapTo(HashSet()) { it.url }
    }

    fun commitLazyTasks(mode: FetchMode, urls: Collection<String>) {
        urls.map { WebPage.newWebPage(it) }.forEach { lazyFetch(it, mode) }

        // Urls with different fetch mode are indexed in different pages and the page number is just the enum ordinal
        val pageNo = LAZY_FETCH_URLS_PAGE_BASE + mode.ordinal
        urlTrackerIndexer.indexAll(pageNo, urls)
        webDb.flush()
    }

    fun getLazyTasks(mode: FetchMode): Set<CharSequence> {
        val pageNo = LAZY_FETCH_URLS_PAGE_BASE + mode.ordinal
        return urlTrackerIndexer.getAll(pageNo)
    }

    fun takeLazyTasks(mode: FetchMode, n: Int): Set<CharSequence> {
        val pageNo = LAZY_FETCH_URLS_PAGE_BASE + mode.ordinal
        return urlTrackerIndexer.takeN(pageNo, n)
    }

    fun commitTimeoutTasks(urls: Collection<CharSequence>, mode: FetchMode) {
        urlTrackerIndexer.indexAll(TIMEOUT_URLS_PAGE, urls)
    }

    fun takeTimeoutTasks(mode: FetchMode): Set<CharSequence> {
        return urlTrackerIndexer.takeAll(TIMEOUT_URLS_PAGE)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            commitLazyTasks()
        }
    }

    private fun lazyFetch(page: WebPage, mode: FetchMode) {
        page.fetchMode = mode
        webDb.put(page)
    }

    private fun commitLazyTasks() {
        val trackingUrls = timeoutUrls.size + failedUrls.size + deadUrls.size
        if (trackingUrls == 0) return

        log.info("Commit urls, timeout: {} failed: {} dead: {}", timeoutUrls.size, failedUrls.size, deadUrls.size)

        // Trace failed tasks for further fetch as a background tasks
        timeoutUrls.takeIf { it.isNotEmpty() }?.let { urlTrackerIndexer.indexAll(TIMEOUT_URLS_PAGE, it) }
        failedUrls.takeIf { it.isNotEmpty() }?.let { urlTrackerIndexer.indexAll(FAILED_URLS_PAGE, it) }
        deadUrls.takeIf { it.isNotEmpty() }?.let { urlTrackerIndexer.indexAll(DEAD_URLS_PAGE, it) }

        urlTrackerIndexer.commit()
    }
}
