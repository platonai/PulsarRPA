package ai.platon.pulsar.common.collect.queue

import ai.platon.pulsar.common.collect.ExternalUrlLoader
import ai.platon.pulsar.common.collect.UrlTopic
import ai.platon.pulsar.common.urls.UrlAware
import org.apache.commons.collections4.map.PassiveExpiringMap
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class ConcurrentLoadingQueue(
    loader: ExternalUrlLoader,
    topic: UrlTopic,
    transformer: (UrlAware) -> UrlAware = { it }
) : DelayLoadingQueue(loader, topic, transformer = transformer)

class ConcurrentNonReentrantLoadingQueue(
    loader: ExternalUrlLoader,
    topic: UrlTopic,
    ttl: Duration = Duration.ofDays(1),
    transformer: (UrlAware) -> UrlAware = { it }
) : DelayLoadingQueue(loader, topic, transformer = transformer) {
    private val historyHash = PassiveExpiringMap<Int, Int>(ttl.toMillis())

    @Synchronized
    fun countHistory(url: UrlAware) = if (countHistory(url.hashCode()) > 0) 1 else 0

    @Synchronized
    fun countHistory(hashCode: Int): Int {
        return historyHash[hashCode] ?: 0
    }

    @Synchronized
    override fun offer(url: UrlAware): Boolean {
        val hashCode = url.hashCode()

        if (countHistory(hashCode) == 0) {
            return if (!url.isPersistable || freeSlots > 0) {
                historyHash[hashCode] = 1
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
    topic: UrlTopic,
    val n: Int = 3,
    val ttl: Duration = Duration.ofDays(1),
    transformer: (UrlAware) -> UrlAware = { it }
) : DelayLoadingQueue(loader, topic, transformer = transformer) {

    // private val historyHash = HashMultiset.create<Int>()

    private val historyHash = PassiveExpiringMap<Int, AtomicInteger>(ttl.toMillis())

    @Synchronized
    fun countHistory(url: UrlAware) = countHistory(url.hashCode())

    @Synchronized
    fun countHistory(hashCode: Int): Int {
        return historyHash[hashCode]?.get() ?: 0
    }

    @Synchronized
    override fun offer(url: UrlAware): Boolean {
        val hashCode = url.hashCode()
        if (countHistory(hashCode) <= n) {
            return if (!url.isPersistable || freeSlots > 0) {
                historyHash.computeIfAbsent(hashCode) { AtomicInteger() }.incrementAndGet()
                urlCache.add(url)
            } else {
                overflow(url)
                true
            }
        }

        return false
    }
}
