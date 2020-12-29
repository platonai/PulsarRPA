package ai.platon.pulsar.common.collect

import java.util.*
import kotlin.NoSuchElementException

open class ConcurrentLoadingIterable<T>(
        val collector: DataCollector<T>,
        val cacheSize: Int = 20
): Iterable<T> {

    private val cache = Collections.synchronizedList(LinkedList<T>())

    override fun iterator() = LoadingIterator(this)

    /**
     * add an item to the very beginning of the fetch queue
     * */
    fun addFirst(e: T) {
        cache.add(0, e)
    }

    class LoadingIterator<T>(
            private val iterable: ConcurrentLoadingIterable<T>
    ): Iterator<T> {
        private val collector get() = iterable.collector

        @Synchronized
        fun tryLoad() {
            if (collector.hasMore() && iterable.cache.size < iterable.cacheSize) {
                collector.collectTo(iterable.cache)
            }
        }

        @Synchronized
        override fun hasNext(): Boolean {
            while (iterable.cache.isEmpty() && collector.hasMore()) {
                tryLoad()
            }

            return iterable.cache.isNotEmpty()
        }

        @Synchronized
        override fun next(): T {
            return iterable.cache.removeFirst() ?: throw NoSuchElementException()
        }
    }
}
