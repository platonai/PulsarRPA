package ai.platon.pulsar.common.collect.queue

import ai.platon.pulsar.common.collect.ExternalUrlLoader
import ai.platon.pulsar.common.collect.UrlGroup
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.UrlAware
import java.time.Duration
import java.time.Instant

/**
 * An url queue should be small since every url uses about 1s to fetch
 * */
open class DelayLoadingQueue(
    loader: ExternalUrlLoader,
    group: UrlGroup,
    /**
     * The delay time to load after another load
     * */
    var loadDelay: Duration = Duration.ofSeconds(5),
    var estimateDelay: Duration = Duration.ofSeconds(5),
    transformer: (UrlAware) -> UrlAware
) : AbstractLoadingQueue(loader, group, transformer) {
    private val logger = getLogger(DelayLoadingQueue::class)

    @Volatile
    protected var lastEstimatedExternalSize: Int = -1

    @Volatile
    protected var lastLoadTime = Instant.EPOCH

    @Volatile
    protected var lastEstimateTime = Instant.EPOCH

    @Volatile
    protected var lastBusyTime = Instant.now()

    protected val isIdle
        get() = Duration.between(lastBusyTime, Instant.now()).seconds > 60

    // always access the underlying layer with a delay to reduce possible IO reads
    val realEstimateDelay get() = if (isIdle) Duration.ofMinutes(1) else estimateDelay

    val isExpired get() = isExpired(loadDelay)

    @get:Synchronized
    override val estimatedExternalSize: Int
        get() = estimateIfExpired().lastEstimatedExternalSize.coerceAtLeast(0)

    fun isExpired(delay: Duration): Boolean {
        return lastLoadTime + delay < Instant.now()
    }

    fun expire() {
        lastLoadTime = Instant.EPOCH
        lastEstimateTime = Instant.EPOCH
    }

    @Synchronized
    override fun load() {
        if (urlCache.isEmpty() && estimatedExternalSize > 0) {
            loadNow()
        } else if (freeSlots > 0 && isExpired) {
            loadNow()
        }
    }

    @Synchronized
    fun load(delay: Duration) {
        if (freeSlots > 0 && isExpired(delay)) {
            loadNow()
        }
    }

    @Synchronized
    override fun loadNow(): Collection<UrlAware> {
        if (freeSlots <= 0) return listOf()

        lastLoadTime = Instant.now()

        val urls = try {
            ++loadCount
            loader.loadToNow(urlCache, freeSlots, group, transformer)
        } catch (e: Exception) {
            logger.warn("Failed to load", e)
            listOf()
        }

        if (urls.isNotEmpty()) {
            lastBusyTime = Instant.now()
            estimateNow()
        }

        return urls
    }

    @Synchronized
    override fun peek(): UrlAware? {
        refreshIfNecessary()
        return urlCache.peek()
    }

    @Synchronized
    override fun poll(): UrlAware? {
        refreshIfNecessary()
        return urlCache.poll()
    }

    private fun estimateIfExpired(): DelayLoadingQueue {
        if (lastEstimateTime + realEstimateDelay < Instant.now()) {
            estimateNow()
        }
        return this
    }

    private fun estimateNow() {
        lastEstimatedExternalSize = externalSize
        lastEstimateTime = Instant.now()
    }

    private fun refreshIfNecessary(): DelayLoadingQueue {
        estimateIfExpired()

        if (urlCache.isEmpty()) {
            load()
        }

        return this
    }
}
