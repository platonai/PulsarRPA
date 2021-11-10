package ai.platon.pulsar.common.collect.queue

import ai.platon.pulsar.common.collect.ExternalUrlLoader
import ai.platon.pulsar.common.collect.UrlTopic
import ai.platon.pulsar.common.getLogger
import ai.platon.pulsar.common.urls.UrlAware
import java.time.Duration
import java.time.Instant

/**
 * A delay loading queue to reduce IO
 * */
open class DelayLoadingQueue(
    loader: ExternalUrlLoader,
    topic: UrlTopic,
    /**
     * The delay time to load after another load
     * */
    var loadDelay: Duration = Duration.ofSeconds(3),
    var estimateDelay: Duration = Duration.ofSeconds(3),
    transformer: (UrlAware) -> UrlAware
) : AbstractLoadingQueue(loader, topic, transformer) {
    private val logger = getLogger(DelayLoadingQueue::class)

    @Volatile
    protected var lastEstimatedExternalSize: Int = -1

    @Volatile
    protected var lastEstimateTime = Instant.EPOCH

    @Volatile
    protected var lastLoadTime = Instant.EPOCH

    /**
     * The last time we loaded something
     * */
    @Volatile
    protected var lastReapedTime = Instant.now()

    /**
     * The time since last reaped time
     * */
    val idleTime get() = Duration.between(lastReapedTime, Instant.now())

    val isIdle get() = idleTime.seconds > 60

    val isBusy get() = !isIdle

    // always access the underlying layer with a delay to reduce possible IO reads
    val adjustedEstimateDelay get() = if (isIdle) Duration.ofSeconds(15) else estimateDelay

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
        lastReapedTime = Instant.now()
    }

    @Synchronized
    override fun load() {
        if (cacheImplementation.isEmpty() && estimatedExternalSize > 0) {
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
            loader.loadToNow(cacheImplementation, freeSlots, topic, transformer)
        } catch (e: Exception) {
            logger.warn("Failed to load", e)
            listOf()
        }

        if (urls.isNotEmpty()) {
            lastReapedTime = Instant.now()
            estimateNow()
        }

        return urls
    }

    @Synchronized
    override fun peek(): UrlAware? {
        refreshIfNecessary()
        return cacheImplementation.peek()
    }

    @Synchronized
    override fun poll(): UrlAware? {
        refreshIfNecessary()
        return cacheImplementation.poll()
    }

    private fun estimateIfExpired(): DelayLoadingQueue {
        if (lastEstimateTime + adjustedEstimateDelay < Instant.now()) {
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

        if (cacheImplementation.isEmpty()) {
            load()
        }

        return this
    }
}
