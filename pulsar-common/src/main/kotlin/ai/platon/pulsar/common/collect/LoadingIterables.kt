package ai.platon.pulsar.common.collect

import java.util.*
import kotlin.NoSuchElementException

open class ConcurrentLoadingIterable<E>(
        val collector: DataCollector<E>,
        val realTimeCollector: DataCollector<E>? = null,
        val cacheSize: Int = 20
): Iterable<E> {

    private val cache = Collections.synchronizedList(LinkedList<E>())

    override fun iterator() = LoadingIterator(this)

    /**
     * add an item to the very beginning of the fetch queue
     * TODO: use realTimeCollector instead
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
            if (collector.hasMore() && cache.size < iterable.cacheSize) {
                collector.collectTo(cache)
            }
        }

        @Synchronized
        override fun hasNext(): Boolean {
            if (realTimeCollector != null && realTimeCollector.hasMore()) {
                collectTo(0, realTimeCollector, cache)
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

        private fun collectTo(index: Int, collector: DataCollector<E>, sink: MutableList<E>): Int {
            val list = mutableListOf<E>()
            collector.collectTo(list)
            sink.addAll(index, list)
            return list.size
        }
    }
}
