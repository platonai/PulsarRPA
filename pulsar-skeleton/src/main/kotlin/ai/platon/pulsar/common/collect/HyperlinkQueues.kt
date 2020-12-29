package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority
import ai.platon.pulsar.common.url.UrlAware
import com.google.common.collect.ConcurrentHashMultiset
import java.util.concurrent.ConcurrentSkipListSet

class ConcurrentLoadingQueue(
        loader: ExternalUrlLoader,
        group: Int = ConcurrentLoadingQueue::javaClass.name.hashCode(),
        priority: Int = Priority.NORMAL.value,
        capacity: Int = 100_000
): AbstractLoadingQueue(loader, group, priority, capacity)

class ConcurrentNonReentrantLoadingQueue(
        loader: ExternalUrlLoader,
        group: Int = ConcurrentNonReentrantLoadingQueue::javaClass.name.hashCode(),
        priority: Int = Priority.NORMAL.value,
        capacity: Int = 100_000
): AbstractLoadingQueue(loader, group, priority, capacity) {
    private val historyHash = ConcurrentSkipListSet<Int>()

    override fun offer(url: UrlAware): Boolean {
        val hashCode = url.hashCode()

        synchronized(this) {
            if (!historyHash.contains(hashCode)) {
                return if (cache.size < capacity) {
                    historyHash.add(hashCode)
                    cache.add(url)
                } else {
                    loader.save(url, group)
                    true
                }
            }
        }

        return false
    }
}

class ConcurrentNEntrantLoadingQueue(
        loader: ExternalUrlLoader,
        val n: Int = 3,
        group: Int = ConcurrentNEntrantLoadingQueue::javaClass.name.hashCode(),
        priority: Int = Priority.NORMAL.value,
        capacity: Int = 100_000
): AbstractLoadingQueue(loader, group, priority, capacity) {

    private val historyHash = ConcurrentHashMultiset.create<Int>()

    override fun offer(url: UrlAware): Boolean {
        val hashCode = url.hashCode()

        synchronized(this) {
            if (historyHash.count(hashCode) <= n) {
                return if (cache.size < capacity) {
                    historyHash.add(hashCode)
                    cache.add(url)
                } else {
                    loader.save(url, group)
                    true
                }
            }
        }

        return false
    }
}
