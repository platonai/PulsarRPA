package ai.platon.pulsar.common.collect

import java.util.*
import kotlin.NoSuchElementException

open class ConcurrentLoadingIterable<E>(
        val collector: DataCollector<E>,
        val realTimeCollector: DataCollector<E>? = null,
        val lowerCacheSize: Int = 20,
        val upperCacheSize: Int = 1_000_000
): Iterable<E> {

    private val cache = Collections.synchronizedList(LinkedList<E>())

    /**
     * Total number of loaded items
     * */
    val cacheSize get() = cache.size

    override fun iterator() = LoadingIterator(this)

    /**
     * add an item to the very beginning of the fetch queue
     * */
    fun addHead(e: E) {
        cache.add(0, e)
    }

    class LoadingIterator<E>(
            private val iterable: ConcurrentLoadingIterable<E>
    ): Iterator<E> {
        private val collector = iterable.collector
        private val realTimeCollector = iterable.realTimeCollector
        private val cache = iterable.cache

        @Synchronized
        fun tryLoad() {
            if (collector.hasMore() && cache.size < iterable.lowerCacheSize) {
                collector.collectTo(cache)
            }
        }

        @Synchronized
        override fun hasNext(): Boolean {
            if (realTimeCollector != null && realTimeCollector.hasMore()) {
                realTimeCollector.collectTo(0, cache)
            }

            while (cache.isEmpty() && collector.hasMore()) {
                tryLoad()
            }

            return cache.isNotEmpty()
        }

        @Synchronized
        override fun next(): E {
            return cache.removeFirst() ?: throw NoSuchElementException()
        }
    }
}
