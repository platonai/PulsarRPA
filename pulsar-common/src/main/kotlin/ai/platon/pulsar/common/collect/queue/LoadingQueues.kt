package ai.platon.pulsar.common.collect.queue

import ai.platon.pulsar.common.collect.ExternalUrlLoader
import ai.platon.pulsar.common.collect.UrlGroup
import ai.platon.pulsar.common.collect.queue.DelayLoadingQueue
import ai.platon.pulsar.common.urls.UrlAware
import com.google.common.collect.HashMultiset

class ConcurrentLoadingQueue(
    loader: ExternalUrlLoader,
    group: UrlGroup,
    transformer: (UrlAware) -> UrlAware = { it }
) : DelayLoadingQueue(loader, group, transformer = transformer)

class ConcurrentNonReentrantLoadingQueue(
    loader: ExternalUrlLoader,
    group: UrlGroup,
    transformer: (UrlAware) -> UrlAware = { it }
) : DelayLoadingQueue(loader, group, transformer = transformer) {
    private val historyHash = HashSet<Int>()

    @Synchronized
    fun count(url: UrlAware) = if (historyHash.contains(url.hashCode())) 1 else 0

    @Synchronized
    override fun offer(url: UrlAware): Boolean {
        val hashCode = url.hashCode()

        if (!historyHash.contains(hashCode)) {
            return if (!url.isPersistable || freeSlots > 0) {
                historyHash.add(hashCode)
                urlCache.add(url)
            } else {
                overflow(url)
                true
            }
        }

        return false
    }
}

class ConcurrentNEntrantLoadingQueue(
    loader: ExternalUrlLoader,
    group: UrlGroup,
    val n: Int = 3,
    transformer: (UrlAware) -> UrlAware = { it }
) : DelayLoadingQueue(loader, group, transformer = transformer) {

    private val historyHash = HashMultiset.create<Int>()

    @Synchronized
    fun count(url: UrlAware) = historyHash.count(url.hashCode())

    @Synchronized
    override fun offer(url: UrlAware): Boolean {
        val hashCode = url.hashCode()

        if (historyHash.count(hashCode) <= n) {
            return if (!url.isPersistable || freeSlots > 0) {
                historyHash.add(hashCode)
                urlCache.add(url)
            } else {
                overflow(url)
                true
            }
        }

        return false
    }
}
