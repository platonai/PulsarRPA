package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.url.UrlAware
import com.google.common.collect.ConcurrentHashMultiset
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet

abstract class AbstractLoadingUrlQueue(val loader: ExternalUrlLoader): AbstractQueue<UrlAware>() {
    protected val set = ConcurrentSkipListSet<UrlAware>()

    override fun add(url: UrlAware) = offer(url)

    @Synchronized
    override fun offer(url: UrlAware): Boolean {
        return if (set.size < loader.cacheSize) {
            set.add(url)
        } else {
            loader.save(url)
            true
        }
    }

    override fun iterator(): MutableIterator<UrlAware> = set.iterator()

    override fun peek(): UrlAware? {
        var url = set.firstOrNull()
        while (url == null && loader.hasMore()) {
            loader.loadTo(set)
            url = set.firstOrNull()
        }
        return url
    }

    override fun poll(): UrlAware? = set.pollFirst()

    override val size: Int get() = set.size
}

class ConcurrentReentrantLoadingUrlQueue(
        loader: ExternalUrlLoader
): AbstractLoadingUrlQueue(loader)

class ConcurrentNonReentrantLoadingUrlQueue(
        loader: ExternalUrlLoader
): AbstractLoadingUrlQueue(loader) {
    private val historyHash = ConcurrentSkipListSet<Int>()

    override fun offer(url: UrlAware): Boolean {
        val hashCode = url.hashCode()

        synchronized(this) {
            if (!historyHash.contains(hashCode)) {
                historyHash.add(hashCode)
                return if (set.size < loader.cacheSize) {
                    set.add(url)
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
        val n: Int
): AbstractLoadingUrlQueue(loader) {

    private val historyHash = ConcurrentHashMultiset.create<Int>()

    override fun offer(url: UrlAware): Boolean {
        val hashCode = url.hashCode()

        synchronized(this) {
            if (historyHash.count(hashCode) <= n) {
                historyHash.add(hashCode)
                return if (set.size < loader.cacheSize) {
                    set.add(url)
                } else {
                    loader.save(url)
                    true
                }
            }
        }

        return false
    }
}
