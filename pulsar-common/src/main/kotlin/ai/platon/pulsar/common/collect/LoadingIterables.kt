package ai.platon.pulsar.common.collect

import java.util.concurrent.ConcurrentLinkedQueue

open class ConcurrentLoadingIterable<T>(
        val collector: DataCollector<T>,
        val cacheSize: Int = 200
): Iterable<T> {

    private val cache = ConcurrentLinkedQueue<T>()

    override fun iterator() = LoadingIterator(this)

    class LoadingIterator<T>(
            private val iterable: ConcurrentLoadingIterable<T>
    ): Iterator<T> {

        @Synchronized
        fun tryLoad() {
            if (iterable.collector.hasMore() && iterable.cache.size < iterable.cacheSize) {
                iterable.collector.collectTo(iterable.cache)
            }
        }

        @Synchronized
        override fun hasNext(): Boolean {
            while (iterable.cache.isEmpty() && iterable.collector.hasMore()) {
                tryLoad()
            }

            return iterable.cache.isNotEmpty()
        }

        @Synchronized
        override fun next(): T {
            return iterable.cache.poll()?:throw NoSuchElementException()
        }
    }
}
