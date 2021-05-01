package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.urls.UrlAware
import com.google.common.collect.HashMultiset

class ConcurrentLoadingQueue(
    loader: ExternalUrlLoader,
    group: UrlGroup,
    capacity: Int = LoadingQueue.DEFAULT_CAPACITY,
) : AbstractLoadingQueue(loader, group, capacity)

class ConcurrentNonReentrantLoadingQueue(
    loader: ExternalUrlLoader,
    group: UrlGroup,
    capacity: Int = LoadingQueue.DEFAULT_CAPACITY,
) : AbstractLoadingQueue(loader, group, capacity) {
    private val historyHash = HashSet<Int>()

    @Synchronized
    fun count(url: UrlAware) = if (historyHash.contains(url.hashCode())) 1 else 0

    @Synchronized
    override fun offer(url: UrlAware): Boolean {
        val hashCode = url.hashCode()

        if (!historyHash.contains(hashCode)) {
            return if (!url.isPersistable || freeSlots > 0) {
                historyHash.add(hashCode)
                implementation.add(url)
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
    capacity: Int = LoadingQueue.DEFAULT_CAPACITY,
) : AbstractLoadingQueue(loader, group, capacity) {

    private val historyHash = HashMultiset.create<Int>()

    @Synchronized
    fun count(url: UrlAware) = historyHash.count(url.hashCode())

    @Synchronized
    override fun offer(url: UrlAware): Boolean {
        val hashCode = url.hashCode()

        if (historyHash.count(hashCode) <= n) {
            return if (!url.isPersistable || freeSlots > 0) {
                historyHash.add(hashCode)
                implementation.add(url)
            } else {
                overflow(url)
                true
            }
        }

        return false
    }
}
