package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.url.UrlAware
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
