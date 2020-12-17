package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.url.UrlAware
import com.google.common.collect.ConcurrentHashMultiset
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet

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
