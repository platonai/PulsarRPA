package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.url.UrlAware
import com.google.common.collect.ConcurrentHashMultiset
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet

class ConcurrentReentrantLoadingUrlQueue(
        val loader: ExternalUrlLoader,
        val cacheSize: Int = 1000000
): AbstractQueue<UrlAware>() {
    private val set = ConcurrentSkipListSet<UrlAware>()

    init {
        loader.loadTo(set)
    }

    override fun add(url: UrlAware) = offer(url)

    override fun offer(e: UrlAware): Boolean {
        synchronized(this) {
            return if (set.size < cacheSize) {
                set.add(e)
            } else {
                loader.save(e)
                true
            }
        }
    }

    override fun iterator(): MutableIterator<UrlAware> = set.iterator()

    override fun peek(): UrlAware? = set.firstOrNull()

    override fun poll(): UrlAware? = set.pollFirst()

    override val size: Int get() = set.size
}

class ConcurrentNonReentrantLoadingUrlQueue(
        val loader: ExternalUrlLoader,
        val cacheSize: Int = 1000000
): AbstractQueue<UrlAware>() {
    private val set = ConcurrentSkipListSet<UrlAware>()
    private val historyHash = ConcurrentSkipListSet<Int>()

    init {
        loader.loadTo(set)
    }

    override fun add(url: UrlAware) = offer(url)

    override fun offer(e: UrlAware): Boolean {
        val hashCode = e.hashCode()

        synchronized(this) {
            if (!historyHash.contains(hashCode)) {
                historyHash.add(hashCode)
                return if (set.size < cacheSize) {
                    set.add(e)
                } else {
                    loader.save(e)
                    true
                }
            }
        }

        return false
    }

    override fun iterator(): MutableIterator<UrlAware> = set.iterator()

    override fun peek(): UrlAware? = set.firstOrNull()

    override fun poll(): UrlAware? = set.pollFirst()

    override val size: Int get() = set.size
}

class ConcurrentNEntrantLoadingUrlQueue(
        val loader: ExternalUrlLoader,
        val n: Int,
        val cacheSize: Int = 1000000,
): AbstractQueue<UrlAware>() {

    private val set = ConcurrentSkipListSet<UrlAware>()

    private val historyHash = ConcurrentHashMultiset.create<Int>()

    init {
        loader.loadTo(set)
    }

    fun count(url: UrlAware) = historyHash.count(url.hashCode())

    override fun add(url: UrlAware) = offer(url)

    override fun offer(url: UrlAware): Boolean {
        val hashCode = url.hashCode()

        synchronized(this) {
            if (historyHash.count(hashCode) <= n) {
                historyHash.add(hashCode)
                return if (set.size < cacheSize) {
                    set.add(url)
                } else {
                    loader.save(url)
                    true
                }
            }
        }

        return false
    }

    override fun iterator(): MutableIterator<UrlAware> = set.iterator()

    override fun peek(): UrlAware? = set.firstOrNull()

    override fun poll(): UrlAware? = set.pollFirst()

    override val size: Int get() = set.size
}
