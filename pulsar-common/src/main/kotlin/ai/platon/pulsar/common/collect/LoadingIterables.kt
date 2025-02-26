package ai.platon.pulsar.common.collect

import ai.platon.pulsar.common.collect.collector.DataCollector
import ai.platon.pulsar.common.getLogger
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
     * Total number of loaded items in the cache.
     * */
    val cacheSize get() = cache.size

    /**
     * Total number of loaded items
     * */
    val size: Int get() {
        return regularCollector.size +
                (realTimeCollector?.size ?: 0) +
                (delayCollector?.size ?: 0) +
                cacheSize
    }

    /**
     * The estimated size of the fetch queue, which is the sum of the size of all collectors.
     * When the collector loads urls from external sources, retrieving exact size of the fetch queue
     * is not possible. So we have to estimate the size.
     * */
    val estimatedSize: Int get() {
        return regularCollector.estimatedSize +
                (realTimeCollector?.estimatedSize ?: 0) +
                (delayCollector?.estimatedSize ?: 0) +
                cacheSize
    }

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
            try {
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
            } catch (e: Throwable) {
                getLogger(this).warn("[Unexpected]", e)
            }

            return false
        }

        @Synchronized
        override fun next(): E {
            // do not use MutableList<T>.removeFirst() which can be conflict with List.removeFirst() in JDK-21
            return cache.removeAt(0) ?: throw NoSuchElementException()
        }
    }
}
