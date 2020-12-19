package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority
import ai.platon.pulsar.common.config.ImmutableConfig
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The fetch catch manager
 * */
interface FetchCatchManager {
    /**
     * The priority fetch caches
     * */
    val fetchCaches: MutableMap<Int, FetchCatch>
    val totalFetchItems: Int

    val lowestFetchCache: FetchCatch
    val lowerFetchCache: FetchCatch
    val normalFetchCache: FetchCatch
    val higherFetchCache: FetchCatch
    val highestFetchCache: FetchCatch

    fun initialize()
}

/**
 * The abstract fetch catch manager
 * */
abstract class AbstractFetchCatchManager(val conf: ImmutableConfig): FetchCatchManager {
    private val initialized = AtomicBoolean()
    override val totalFetchItems get() = fetchCaches.values.sumOf { it.totalSize }

    override val lowestFetchCache: FetchCatch get() = ensureInitialized().fetchCaches[Priority.LOWEST.value]!!
    override val lowerFetchCache: FetchCatch get() = ensureInitialized().fetchCaches[Priority.LOWER.value]!!
    override val normalFetchCache: FetchCatch get() = ensureInitialized().fetchCaches[Priority.NORMAL.value]!!
    override val higherFetchCache: FetchCatch get() = ensureInitialized().fetchCaches[Priority.HIGHER.value]!!
    override val highestFetchCache: FetchCatch get() = ensureInitialized().fetchCaches[Priority.HIGHEST.value]!!

    override fun initialize() {
        if (initialized.compareAndSet(false, true)) {
            Priority.values().forEach { fetchCaches[it.value] = ConcurrentFetchCatch(conf) }
        }
    }

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
    override val fetchCaches = ConcurrentSkipListMap<Int, FetchCatch>()
}

class LoadingFetchCatchManager(
        val urlLoader: ExternalUrlLoader,
        conf: ImmutableConfig
): ConcurrentFetchCatchManager(conf) {
    override fun initialize() {
        Priority.values().map { it.value }.forEach { priority ->
            fetchCaches[priority] = LoadingFetchCatch(urlLoader, priority, conf)
        }
    }
}
