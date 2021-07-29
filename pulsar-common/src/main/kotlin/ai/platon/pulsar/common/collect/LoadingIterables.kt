package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.collect.collector.DataCollector
import java.util.*
import kotlin.NoSuchElementException

open class ConcurrentLoadingIterable<E>(
    val regularCollector: DataCollector<E>,
    val realTimeCollector: DataCollector<E>? = null,
    val delayCollector: DataCollector<E>? = null,
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
    fun addFirst(e: E) {
        cache.add(0, e)
    }

    fun addLast(e: E) {
        cache.add(e)
    }

    fun shuffle() {
        cache.shuffle()
    }

    fun clear() {
        realTimeCollector?.clear()
        delayCollector?.clear()
        regularCollector.clear()
        cache.clear()
    }

    class LoadingIterator<E>(
            private val iterable: ConcurrentLoadingIterable<E>
    ): Iterator<E> {
        private val regularCollector = iterable.regularCollector
        private val realTimeCollector = iterable.realTimeCollector
        private val delayCollector = iterable.delayCollector
        private val cache = iterable.cache

        @Synchronized
        fun tryLoad() {
            if (regularCollector.hasMore() && cache.size < iterable.lowerCacheSize) {
                regularCollector.collectTo(cache)
            }
        }

        @Synchronized
        override fun hasNext(): Boolean {
            if (realTimeCollector != null && realTimeCollector.hasMore()) {
                realTimeCollector.collectTo(0, cache)
            }

            if (delayCollector != null && delayCollector.hasMore()) {
                delayCollector.collectTo(0, cache)
            }

            while (cache.isEmpty() && regularCollector.hasMore()) {
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
