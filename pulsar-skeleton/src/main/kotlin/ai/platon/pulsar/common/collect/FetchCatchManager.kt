package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority
import ai.platon.pulsar.common.collect.FetchCatchManager.Companion.REAL_TIME_PRIORITY
import ai.platon.pulsar.common.config.ImmutableConfig
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The fetch catch manager
 * */
interface FetchCatchManager {
    companion object {
        val REAL_TIME_PRIORITY = Priority.HIGHEST.value * 2
    }

    /**
     * The priority fetch caches
     * */
    val caches: MutableMap<Int, FetchCache>
    val totalItems: Int

    val lowestCache: FetchCache
    val lowerCache: FetchCache
    val normalCache: FetchCache
    val higherCache: FetchCache
    val highestCache: FetchCache

    val realTimeCache: FetchCache

    fun initialize()
}

/**
 * The abstract fetch catch manager
 * */
abstract class AbstractFetchCatchManager(val conf: ImmutableConfig): FetchCatchManager {
    protected val initialized = AtomicBoolean()
    override val totalItems get() = ensureInitialized().caches.values.sumOf { it.totalSize }

    override val lowestCache: FetchCache get() = ensureInitialized().caches[Priority.LOWEST.value]!!
    override val lowerCache: FetchCache get() = ensureInitialized().caches[Priority.LOWER.value]!!
    override val normalCache: FetchCache get() = ensureInitialized().caches[Priority.NORMAL.value]!!
    override val higherCache: FetchCache get() = ensureInitialized().caches[Priority.HIGHER.value]!!
    override val highestCache: FetchCache get() = ensureInitialized().caches[Priority.HIGHEST.value]!!

    private fun ensureInitialized(): AbstractFetchCatchManager {
        if (initialized.compareAndSet(false, true)) {
            initialize()
        }
        return this
    }
}

/**
 * The global cache
 * */
open class ConcurrentFetchCatchManager(conf: ImmutableConfig): AbstractFetchCatchManager(conf) {
    /**
     * The priority fetch caches
     * */
    override val caches = ConcurrentSkipListMap<Int, FetchCache>()
    /**
     * The real time fetch cache
     * */
    override val realTimeCache: FetchCache = ConcurrentFetchCache(conf)

    override fun initialize() {
        if (initialized.compareAndSet(false, true)) {
            Priority.values().forEach { caches[it.value] = ConcurrentFetchCache(conf) }
        }
    }
}

class LoadingFetchCatchManager(
        val urlLoader: ExternalUrlLoader,
        val capacity: Int = 10_000,
        conf: ImmutableConfig
): ConcurrentFetchCatchManager(conf) {
    /**
     * The real time fetch cache
     * */
    override val realTimeCache: FetchCache = LoadingFetchCache(urlLoader, REAL_TIME_PRIORITY, capacity, conf)

    override fun initialize() {
        if (initialized.compareAndSet(false, true)) {
            Priority.values().map { it.value }.forEach { priority ->
                caches[priority] = LoadingFetchCache(urlLoader, priority, capacity, conf)
            }
        }
    }
}
