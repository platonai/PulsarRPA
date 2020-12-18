package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.Priority
import ai.platon.pulsar.common.url.UrlAware
import com.google.common.collect.ConcurrentHashMultiset
import java.util.concurrent.ConcurrentSkipListSet

class ConcurrentReentrantLoadingUrlQueue(
        loader: ExternalUrlLoader,
        group: Int = ConcurrentReentrantLoadingUrlQueue::javaClass.name.hashCode(),
        priority: Int = Priority.NORMAL.value
): AbstractLoadingUrlQueue(loader, group, priority)

class ConcurrentNonReentrantLoadingUrlQueue(
        loader: ExternalUrlLoader,
        group: Int = ConcurrentNonReentrantLoadingUrlQueue::javaClass.name.hashCode(),
        priority: Int = Priority.NORMAL.value
): AbstractLoadingUrlQueue(loader, group, priority) {
    private val historyHash = ConcurrentSkipListSet<Int>()

    override fun offer(url: UrlAware): Boolean {
        val hashCode = url.hashCode()

        synchronized(this) {
            if (!historyHash.contains(hashCode)) {
                historyHash.add(hashCode)
                return if (cache.size < loader.cacheSize) {
                    cache.add(url)
                } else {
                    loader.save(url)
                    true
                }
            }
        }

        return false
    }
}

class ConcurrentNEntrantLoadingUrlQueue(
        loader: ExternalUrlLoader,
        val n: Int,
        group: Int = ConcurrentNEntrantLoadingUrlQueue::javaClass.name.hashCode(),
        priority: Int = Priority.NORMAL.value
): AbstractLoadingUrlQueue(loader, group, priority) {

    private val historyHash = ConcurrentHashMultiset.create<Int>()

    override fun offer(url: UrlAware): Boolean {
        val hashCode = url.hashCode()

        synchronized(this) {
            if (historyHash.count(hashCode) <= n) {
                historyHash.add(hashCode)
                return if (cache.size < loader.cacheSize) {
                    cache.add(url)
                } else {
                    loader.save(url)
                    true
                }
            }
        }

        return false
    }
}
